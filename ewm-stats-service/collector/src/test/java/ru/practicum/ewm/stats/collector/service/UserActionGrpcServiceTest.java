package ru.practicum.ewm.stats.collector.service;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.collector.mapper.UserActionMapper;
import ru.practicum.stats.service.collector.ActionTypeProto;
import ru.practicum.stats.service.collector.UserActionProto;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserActionGrpcServiceTest {

    private static final String TOPIC = "stats.user-actions.v1";

    @Mock
    private KafkaTemplate<Long, UserActionAvro> kafkaTemplate;
    @Mock
    private StreamObserver<Empty> responseObserver;

    private UserActionGrpcService service;

    @BeforeEach
    void setUp() {
        service = new UserActionGrpcService(new UserActionMapper(), kafkaTemplate);
        ReflectionTestUtils.setField(service, "userActionsTopic", TOPIC);
    }

    @Test
    void shouldAcknowledgeOnlyAfterKafkaSendSucceeds() {
        CompletableFuture<SendResult<Long, UserActionAvro>> sendResult = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(sendResult);

        service.collectUserAction(action(), responseObserver);

        ArgumentCaptor<UserActionAvro> valueCaptor = ArgumentCaptor.forClass(UserActionAvro.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq(15L), valueCaptor.capture());
        assertEquals(25L, valueCaptor.getValue().getEventId());
        verify(responseObserver).onNext(Empty.getDefaultInstance());
        verify(responseObserver).onCompleted();
    }

    @Test
    void shouldReturnUnavailableWhenKafkaSendFails() {
        CompletableFuture<SendResult<Long, UserActionAvro>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(failed);

        service.collectUserAction(action(), responseObserver);

        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(errorCaptor.capture());
        assertSame(Status.Code.UNAVAILABLE,
                ((StatusRuntimeException) errorCaptor.getValue()).getStatus().getCode());
        verify(responseObserver, never()).onCompleted();
    }

    private UserActionProto action() {
        return UserActionProto.newBuilder()
                .setUserId(15L)
                .setEventId(25L)
                .setActionType(ActionTypeProto.ACTION_LIKE)
                .setTimestamp(Timestamp.newBuilder().setSeconds(1_700_000_000L).build())
                .build();
    }
}
