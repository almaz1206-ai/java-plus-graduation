package ru.practicum.ewm.stats.client;

import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.practicum.stats.service.dashboard.InteractionsCountRequestProto;
import ru.practicum.stats.service.dashboard.RecommendationsControllerGrpc;
import ru.practicum.stats.service.dashboard.RecommendedEventProto;
import ru.practicum.stats.service.dashboard.SimilarEventsRequestProto;
import ru.practicum.stats.service.dashboard.UserPredictionsRequestProto;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class RecommendationsClient {
    private static final Logger log = LoggerFactory.getLogger(RecommendationsClient.class);

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public Stream<RecommendedEvent> getRecommendationsForUser(long userId, int maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        try {
            return toStream(client.getRecommendationsForUser(request), "getRecommendationsForUser");
        } catch (StatusRuntimeException exception) {
            throw clientError("getRecommendationsForUser", exception);
        }
    }

    public Stream<RecommendedEvent> getSimilarEvents(long eventId, long userId, int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        try {
            return toStream(client.getSimilarEvents(request), "getSimilarEvents");
        } catch (StatusRuntimeException exception) {
            throw clientError("getSimilarEvents", exception);
        }
    }

    public Map<Long, Double> getInteractionsCount(Collection<Long> eventIds) {
        Objects.requireNonNull(eventIds, "eventIds");
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventIds(eventIds)
                .build();
        try {
            Map<Long, Double> result = new LinkedHashMap<>();
            client.getInteractionsCount(request).forEachRemaining(event ->
                    result.put(event.getEventId(), event.getScore()));
            return result;
        } catch (StatusRuntimeException exception) {
            throw clientError("getInteractionsCount", exception);
        }
    }

    private Stream<RecommendedEvent> toStream(Iterator<RecommendedEventProto> iterator, String operation) {
        Iterator<RecommendedEventProto> safeIterator = new Iterator<>() {
            @Override
            public boolean hasNext() {
                try {
                    return iterator.hasNext();
                } catch (StatusRuntimeException exception) {
                    throw clientError(operation, exception);
                }
            }

            @Override
            public RecommendedEventProto next() {
                try {
                    return iterator.next();
                } catch (StatusRuntimeException exception) {
                    throw clientError(operation, exception);
                }
            }
        };
        Spliterator<RecommendedEventProto> spliterator = Spliterators.spliteratorUnknownSize(
                safeIterator, Spliterator.ORDERED | Spliterator.NONNULL);
        return StreamSupport.stream(spliterator, false)
                .map(event -> new RecommendedEvent(event.getEventId(), event.getScore()));
    }

    private StatsClientException clientError(String operation, StatusRuntimeException exception) {
        log.warn("Analyzer gRPC call failed: operation={}, status={}",
                operation, exception.getStatus().getCode());
        return new StatsClientException(operation, exception);
    }
}
