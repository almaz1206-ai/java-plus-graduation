package ru.practicum.ewm.stats.aggregator.service;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimilarityStateTest {

    private static final double EPSILON = 1.0e-9;

    private final SimilarityState state = new SimilarityState();

    @Test
    void firstActionShouldNotCreatePairOrDivideByZero() {
        assertTrue(state.update(action(1, 10, ActionTypeAvro.VIEW, 1000)).isEmpty());
    }

    @Test
    void twoEventsWithOneCommonUserShouldHaveSimilarityOneForEqualWeights() {
        state.update(action(1, 10, ActionTypeAvro.VIEW, 1000));

        EventSimilarityAvro result = only(state.update(action(1, 20, ActionTypeAvro.VIEW, 2000)));

        assertEquals(1.0, result.getScore(), EPSILON);
    }

    @Test
    void shouldCalculateFormulaForSeveralUsers() {
        state.update(action(1, 10, ActionTypeAvro.VIEW, 1000));
        state.update(action(1, 20, ActionTypeAvro.REGISTER, 2000));
        state.update(action(2, 10, ActionTypeAvro.LIKE, 3000));

        EventSimilarityAvro result = only(state.update(action(2, 20, ActionTypeAvro.VIEW, 4000)));

        assertEquals(0.8 / Math.sqrt(1.4 * 1.2), result.getScore(), EPSILON);
    }

    @Test
    void viewToRegisterShouldUpdateNumeratorAndDenominatorIncrementally() {
        state.update(action(1, 10, ActionTypeAvro.VIEW, 1000));
        state.update(action(1, 20, ActionTypeAvro.LIKE, 2000));

        EventSimilarityAvro result = only(state.update(action(1, 10, ActionTypeAvro.REGISTER, 3000)));

        assertEquals(0.8 / Math.sqrt(0.8), result.getScore(), EPSILON);
    }

    @Test
    void registerToLikeShouldUpdateSimilarity() {
        state.update(action(1, 10, ActionTypeAvro.REGISTER, 1000));
        state.update(action(1, 20, ActionTypeAvro.LIKE, 2000));

        EventSimilarityAvro result = only(state.update(action(1, 10, ActionTypeAvro.LIKE, 3000)));

        assertEquals(1.0, result.getScore(), EPSILON);
    }

    @Test
    void weakerActionAfterStrongerShouldNotChangeState() {
        state.update(action(1, 10, ActionTypeAvro.LIKE, 1000));

        assertTrue(state.update(action(1, 10, ActionTypeAvro.VIEW, 2000)).isEmpty());
    }

    @Test
    void repeatedActionShouldNotRecalculateSimilarity() {
        state.update(action(1, 10, ActionTypeAvro.VIEW, 1000));
        state.update(action(1, 20, ActionTypeAvro.VIEW, 2000));

        assertTrue(state.update(action(1, 20, ActionTypeAvro.VIEW, 3000)).isEmpty());
    }

    @Test
    void shouldNormalizePairAndNeverProduceReversePair() {
        state.update(action(1, 20, ActionTypeAvro.VIEW, 1000));

        List<EventSimilarityAvro> updates = state.update(action(1, 10, ActionTypeAvro.VIEW, 2000));

        EventSimilarityAvro result = only(updates);
        assertEquals(10L, result.getEventA());
        assertEquals(20L, result.getEventB());
        assertTrue(updates.stream().noneMatch(value -> value.getEventA() == 20L && value.getEventB() == 10L));
    }

    @Test
    void shouldUseTimestampOfActionThatTriggeredRecalculation() {
        state.update(action(1, 10, ActionTypeAvro.VIEW, 1000));

        EventSimilarityAvro result = only(state.update(action(1, 20, ActionTypeAvro.VIEW, 9876)));

        assertEquals(9876L, result.getTimestamp().toEpochMilli());
    }

    @Test
    void existingPairShouldNotBePublishedWithoutCommonUserForCurrentAction() {
        state.update(action(1, 10, ActionTypeAvro.VIEW, 1000));
        state.update(action(1, 20, ActionTypeAvro.VIEW, 2000));

        List<EventSimilarityAvro> updates = state.update(action(2, 10, ActionTypeAvro.LIKE, 3000));

        assertTrue(updates.isEmpty());
    }

    @Test
    void newPairWithoutCommonUserShouldNotBeCreated() {
        state.update(action(1, 10, ActionTypeAvro.VIEW, 1000));

        List<EventSimilarityAvro> updates = state.update(action(2, 20, ActionTypeAvro.LIKE, 2000));

        assertTrue(updates.isEmpty());
    }

    @Test
    void shouldPublishOnlyNewPairForCommonUser() {
        state.update(action(1, 10, ActionTypeAvro.VIEW, 1000));
        state.update(action(1, 30, ActionTypeAvro.VIEW, 2000));
        state.update(action(2, 20, ActionTypeAvro.REGISTER, 3000));

        List<EventSimilarityAvro> updates = state.update(action(2, 10, ActionTypeAvro.LIKE, 4000));

        assertEquals(1, updates.size());
        assertPair(updates.get(0), 10, 20);
        assertTrue(updates.stream().allMatch(value -> value.getTimestamp().toEpochMilli() == 4000L));
    }

    @Test
    void shouldPublishExistingPairOnlyWhenCurrentUserInteractsWithBothEvents() {
        state.update(action(1, 6, ActionTypeAvro.VIEW, 1000));
        assertEquals(1, state.update(action(1, 8, ActionTypeAvro.REGISTER, 2000)).size());

        assertTrue(state.update(action(2, 6, ActionTypeAvro.LIKE, 3000)).isEmpty());

        List<EventSimilarityAvro> updates = state.update(action(2, 8, ActionTypeAvro.VIEW, 4000));

        assertEquals(1, updates.size());
        assertPair(updates.getFirst(), 6, 8);
        assertEquals(4000L, updates.getFirst().getTimestamp().toEpochMilli());
    }

    @Test
    void publicationFailureShouldRollbackStateForRedelivery() {
        state.update(action(1, 10, ActionTypeAvro.VIEW, 1000));
        UserActionAvro secondEvent = action(1, 20, ActionTypeAvro.VIEW, 2000);

        assertThrows(IllegalStateException.class,
                () -> state.process(secondEvent, ignored -> {
                    throw new IllegalStateException("Kafka unavailable");
                }));

        assertEquals(1, state.update(secondEvent).size());
    }

    private UserActionAvro action(long userId, long eventId, ActionTypeAvro type, long timestamp) {
        return UserActionAvro.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(type)
                .setTimestamp(Instant.ofEpochMilli(timestamp))
                .build();
    }

    private EventSimilarityAvro only(List<EventSimilarityAvro> updates) {
        assertEquals(1, updates.size());
        return updates.getFirst();
    }

    private void assertPair(EventSimilarityAvro similarity, long eventA, long eventB) {
        assertEquals(eventA, similarity.getEventA());
        assertEquals(eventB, similarity.getEventB());
    }
}
