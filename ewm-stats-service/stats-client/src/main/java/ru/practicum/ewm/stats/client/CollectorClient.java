package ru.practicum.ewm.stats.client;

import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.practicum.stats.service.collector.ActionTypeProto;
import ru.practicum.stats.service.collector.UserActionControllerGrpc;
import ru.practicum.stats.service.collector.UserActionProto;

import java.time.Instant;
import java.util.Objects;

@Component
public class CollectorClient {
    private static final Logger log = LoggerFactory.getLogger(CollectorClient.class);

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void collectUserAction(long userId, long eventId, ActionType actionType, Instant timestamp) {
        Objects.requireNonNull(actionType, "actionType");
        Objects.requireNonNull(timestamp, "timestamp");
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(toProto(actionType))
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(timestamp.getEpochSecond())
                        .setNanos(timestamp.getNano())
                        .build())
                .build();
        try {
            client.collectUserAction(request);
        } catch (StatusRuntimeException exception) {
            throw clientError("collectUserAction", exception);
        }
    }

    private ActionTypeProto toProto(ActionType actionType) {
        return switch (actionType) {
            case VIEW -> ActionTypeProto.ACTION_VIEW;
            case REGISTER -> ActionTypeProto.ACTION_REGISTER;
            case LIKE -> ActionTypeProto.ACTION_LIKE;
        };
    }

    private StatsClientException clientError(String operation, StatusRuntimeException exception) {
        log.warn("Collector gRPC call failed: operation={}, status={}",
                operation, exception.getStatus().getCode());
        return new StatsClientException(operation, exception);
    }
}
