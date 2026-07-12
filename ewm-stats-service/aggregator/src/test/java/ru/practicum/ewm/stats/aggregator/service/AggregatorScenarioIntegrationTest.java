package ru.practicum.ewm.stats.aggregator.service;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AggregatorScenarioIntegrationTest {

    private static final double EPSILON = 1.0e-12;

    @Test
    void shouldCalculateScenarioIncrementallyFromAvroActions() {
        SimilarityState state = new SimilarityState();

        assertTrue(state.update(action(1L, 10L, ActionTypeAvro.VIEW, 1)).isEmpty());
        List<EventSimilarityAvro> firstPair = state.update(action(1L, 20L, ActionTypeAvro.LIKE, 2));
        assertSimilarity(firstPair, 0.4 / Math.sqrt(0.4 * 1.0), 2);

        state.update(action(2L, 10L, ActionTypeAvro.REGISTER, 3));
        List<EventSimilarityAvro> initial = state.update(action(2L, 20L, ActionTypeAvro.VIEW, 4));
        assertSimilarity(initial, 0.8 / Math.sqrt(1.2 * 1.4), 4);

        List<EventSimilarityAvro> upgraded = state.update(action(1L, 10L, ActionTypeAvro.LIKE, 5));
        assertSimilarity(upgraded, 1.4 / Math.sqrt(1.8 * 1.4), 5);

        assertTrue(state.update(action(1L, 10L, ActionTypeAvro.VIEW, 6)).isEmpty());
        assertTrue(state.update(action(1L, 10L, ActionTypeAvro.LIKE, 7)).isEmpty());
    }

    private void assertSimilarity(List<EventSimilarityAvro> values, double expectedScore, long timestampMillis) {
        assertEquals(1, values.size());
        EventSimilarityAvro value = values.get(0);
        assertEquals(10L, value.getEventA());
        assertEquals(20L, value.getEventB());
        assertEquals(expectedScore, value.getScore(), EPSILON);
        assertEquals(Instant.ofEpochMilli(timestampMillis), value.getTimestamp());
    }

    private UserActionAvro action(long userId, long eventId, ActionTypeAvro type, long timestampMillis) {
        return UserActionAvro.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(type)
                .setTimestamp(Instant.ofEpochMilli(timestampMillis))
                .build();
    }
}
