package ru.practicum.ewm.stats.analyzer.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.support.Acknowledgment;
import ru.practicum.ewm.stats.analyzer.service.AnalyzerPersistenceService;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnalyzerKafkaListenerTest {

    @Mock
    private AnalyzerPersistenceService persistenceService;
    @Mock
    private Acknowledgment acknowledgment;

    @Test
    void shouldAcknowledgeUserActionAfterSuccessfulTransaction() {
        UserActionAvro action = action();

        new AnalyzerKafkaListener(persistenceService).consumeUserAction(action, acknowledgment);

        verify(persistenceService).process(action);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldNotAcknowledgeUserActionWhenDatabaseIsUnavailable() {
        UserActionAvro action = action();
        doThrow(new DataAccessResourceFailureException("database unavailable"))
                .when(persistenceService).process(action);

        assertThrows(DataAccessResourceFailureException.class,
                () -> new AnalyzerKafkaListener(persistenceService).consumeUserAction(action, acknowledgment));
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void shouldAcknowledgeSimilarityAfterSuccessfulTransaction() {
        EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                .setEventA(1L).setEventB(2L).setScore(0.5).setTimestamp(Instant.EPOCH).build();

        new AnalyzerKafkaListener(persistenceService).consumeSimilarity(similarity, acknowledgment);

        verify(persistenceService).process(similarity);
        verify(acknowledgment).acknowledge();
    }

    private UserActionAvro action() {
        return UserActionAvro.newBuilder()
                .setUserId(1L).setEventId(2L).setActionType(ActionTypeAvro.VIEW)
                .setTimestamp(Instant.EPOCH).build();
    }
}
