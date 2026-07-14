package ru.practicum.ewm.stats.collector.service;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.collector.mapper.UserActionMapper;
import ru.practicum.stats.service.collector.UserActionControllerGrpc;
import ru.practicum.stats.service.collector.UserActionProto;

import java.util.concurrent.ExecutionException;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionGrpcService extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final UserActionMapper mapper;
    private final KafkaTemplate<Long, UserActionAvro> kafkaTemplate;

    @Value("${collector.kafka.user-actions-topic}")
    private String userActionsTopic;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            UserActionAvro action = mapper.map(request);
            kafkaTemplate.send(userActionsTopic, action.getUserId(), action).get();
            log.debug("Published user action type={} to topic={}", action.getActionType(), userActionsTopic);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .withCause(exception)
                    .asRuntimeException());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Kafka publication interrupted for topic={}", userActionsTopic);
            responseObserver.onError(Status.UNAVAILABLE
                    .withDescription("Action publication interrupted")
                    .withCause(exception)
                    .asRuntimeException());
        } catch (ExecutionException | RuntimeException exception) {
            log.error("Failed to publish user action to topic={}", userActionsTopic, exception);
            responseObserver.onError(Status.UNAVAILABLE
                    .withDescription("Failed to publish user action")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }
}
