package ru.practicum.ewm.stats.aggregator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserActionKafkaListenerTest {

    @Mock
    private SimilarityState state;
    @Mock
    private KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;
    @Mock
    private Acknowledgment acknowledgment;

    private UserActionKafkaListener listener;

    @BeforeEach
    void setUp() {
        listener = new UserActionKafkaListener(state, kafkaTemplate);
        ReflectionTestUtils.setField(listener, "eventsSimilarityTopic", "stats.events-similarity.v1");
    }

    @Test
    void shouldAcknowledgeOnlyAfterAllPublicationsSucceed() throws Exception {
        UserActionAvro action = action();
        EventSimilarityAvro similarity = similarity();
        doAnswer(invocation -> {
            SimilarityState.SimilarityPublisher publisher = invocation.getArgument(1);
            publisher.publish(List.of(similarity));
            return null;
        }).when(state).process(eq(action), any());
        when(kafkaTemplate.send("stats.events-similarity.v1", "10:20", similarity))
                .thenReturn(CompletableFuture.completedFuture(null));

        listener.consume(action, acknowledgment);

        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldNotAcknowledgeWhenPublicationFails() {
        UserActionAvro action = action();
        EventSimilarityAvro similarity = similarity();
        CompletableFuture<SendResult<String, EventSimilarityAvro>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        try {
            doAnswer(invocation -> {
                SimilarityState.SimilarityPublisher publisher = invocation.getArgument(1);
                publisher.publish(List.of(similarity));
                return null;
            }).when(state).process(eq(action), any());
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
        when(kafkaTemplate.send("stats.events-similarity.v1", "10:20", similarity)).thenReturn(failed);

        assertThrows(Exception.class, () -> listener.consume(action, acknowledgment));
        verify(acknowledgment, never()).acknowledge();
    }

    private UserActionAvro action() {
        return UserActionAvro.newBuilder()
                .setUserId(1L).setEventId(10L).setActionType(ActionTypeAvro.VIEW)
                .setTimestamp(Instant.EPOCH).build();
    }

    private EventSimilarityAvro similarity() {
        return EventSimilarityAvro.newBuilder()
                .setEventA(10L).setEventB(20L).setScore(1.0)
                .setTimestamp(Instant.EPOCH).build();
    }
}
