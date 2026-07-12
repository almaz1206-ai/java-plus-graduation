package ru.practicum.ewm.stats.analyzer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyzerPersistenceServiceTest {

    @Mock
    private UserInteractionRepository interactionRepository;
    @Mock
    private EventSimilarityRepository similarityRepository;

    @Test
    void shouldConvertActionWeightAndPreserveTimestamp() {
        AnalyzerPersistenceService service = service();
        Instant timestamp = Instant.parse("2026-01-01T10:00:00Z");
        UserActionAvro action = UserActionAvro.newBuilder()
                .setUserId(1L).setEventId(2L).setActionType(ActionTypeAvro.REGISTER)
                .setTimestamp(timestamp).build();

        service.process(action);

        verify(interactionRepository).saveMaximumWeight(1L, 2L, 0.8, timestamp);
    }

    @Test
    void shouldNormalizeAndUpsertSimilarity() {
        AnalyzerPersistenceService service = service();
        Instant timestamp = Instant.parse("2026-01-01T10:00:00Z");
        EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                .setEventA(20L).setEventB(10L).setScore(0.75)
                .setTimestamp(timestamp).build();

        service.process(similarity);

        verify(similarityRepository).upsert(10L, 20L, 0.75, timestamp);
    }

    @Test
    void shouldRejectSelfSimilarity() {
        EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                .setEventA(10L).setEventB(10L).setScore(1.0)
                .setTimestamp(Instant.EPOCH).build();

        assertThrows(IllegalArgumentException.class, () -> service().process(similarity));
    }

    @Test
    void shouldPropagateDatabaseFailureForTransactionRollback() {
        UserActionAvro action = UserActionAvro.newBuilder()
                .setUserId(1L).setEventId(2L).setActionType(ActionTypeAvro.LIKE)
                .setTimestamp(Instant.EPOCH).build();
        when(interactionRepository.saveMaximumWeight(1L, 2L, 1.0, Instant.EPOCH))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThrows(DataAccessResourceFailureException.class, () -> service().process(action));
    }

    private AnalyzerPersistenceService service() {
        return new AnalyzerPersistenceService(interactionRepository, similarityRepository);
    }
}
