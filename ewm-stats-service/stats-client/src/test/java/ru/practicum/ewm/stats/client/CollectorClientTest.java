package ru.practicum.ewm.stats.client;

import com.google.protobuf.Empty;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.stats.service.collector.ActionTypeProto;
import ru.practicum.stats.service.collector.UserActionControllerGrpc;
import ru.practicum.stats.service.collector.UserActionProto;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectorClientTest {
    private final UserActionControllerGrpc.UserActionControllerBlockingStub stub = mock(
            UserActionControllerGrpc.UserActionControllerBlockingStub.class);
    private final CollectorClient client = new CollectorClient();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "client", stub);
    }

    @ParameterizedTest
    @EnumSource(ActionType.class)
    void shouldBuildRequestAndMapEveryActionType(ActionType actionType) {
        Instant timestamp = Instant.parse("2026-07-12T12:34:56.123456789Z");
        when(stub.collectUserAction(any())).thenReturn(Empty.getDefaultInstance());

        client.collectUserAction(10L, 20L, actionType, timestamp);

        ArgumentCaptor<UserActionProto> captor = ArgumentCaptor.forClass(UserActionProto.class);
        verify(stub).collectUserAction(captor.capture());
        UserActionProto request = captor.getValue();
        assertEquals(10L, request.getUserId());
        assertEquals(20L, request.getEventId());
        assertEquals(timestamp.getEpochSecond(), request.getTimestamp().getSeconds());
        assertEquals(timestamp.getNano(), request.getTimestamp().getNanos());
        assertEquals(expected(actionType), request.getActionType());
    }

    @Test
    void shouldConvertGrpcFailureToClientException() {
        when(stub.collectUserAction(any())).thenThrow(Status.UNAVAILABLE.asRuntimeException());

        StatsClientException exception = assertThrows(StatsClientException.class,
                () -> client.collectUserAction(1L, 2L, ActionType.VIEW, Instant.EPOCH));

        assertEquals(Status.Code.UNAVAILABLE, exception.getStatusCode());
    }

    private ActionTypeProto expected(ActionType actionType) {
        return switch (actionType) {
            case VIEW -> ActionTypeProto.ACTION_VIEW;
            case REGISTER -> ActionTypeProto.ACTION_REGISTER;
            case LIKE -> ActionTypeProto.ACTION_LIKE;
        };
    }
}
