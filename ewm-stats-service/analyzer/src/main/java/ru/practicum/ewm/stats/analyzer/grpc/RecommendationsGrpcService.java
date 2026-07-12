package ru.practicum.ewm.stats.analyzer.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.EventWeightSumView;
import ru.practicum.ewm.stats.analyzer.repository.CandidateNeighborView;
import ru.practicum.ewm.stats.analyzer.repository.SimilarEventView;
import ru.practicum.ewm.stats.analyzer.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.analyzer.config.RecommendationProperties;
import ru.practicum.stats.service.dashboard.InteractionsCountRequestProto;
import ru.practicum.stats.service.dashboard.RecommendationsControllerGrpc;
import ru.practicum.stats.service.dashboard.RecommendedEventProto;
import ru.practicum.stats.service.dashboard.SimilarEventsRequestProto;
import ru.practicum.stats.service.dashboard.UserPredictionsRequestProto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
@RequiredArgsConstructor
@Transactional(readOnly = true)
@EnableConfigurationProperties(RecommendationProperties.class)
public class RecommendationsGrpcService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final EventSimilarityRepository similarityRepository;
    private final UserInteractionRepository interactionRepository;
    private final RecommendationProperties recommendationProperties;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        if (!validPositive(request.getUserId(), "userId", responseObserver)
                || !validPositive(request.getMaxResults(), "maxResults", responseObserver)) {
            return;
        }
        try {
            List<RecommendedEventProto> results = predict(
                    similarityRepository.findRecommendationNeighbors(
                            request.getUserId(),
                            recommendationProperties.historyLimit(),
                            recommendationProperties.candidateLimit(),
                            recommendationProperties.neighborLimit()),
                    request.getMaxResults());
            results.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (DataAccessException exception) {
            databaseError(responseObserver, exception);
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        if (!validPositive(request.getEventId(), "eventId", responseObserver)
                || !validPositive(request.getUserId(), "userId", responseObserver)
                || !validPositive(request.getMaxResults(), "maxResults", responseObserver)) {
            return;
        }
        try {
            List<RecommendedEventProto> results = similarityRepository.findUnseenSimilarEvents(
                            request.getEventId(), request.getUserId(), PageRequest.of(0, request.getMaxResults()))
                    .stream()
                    .map(this::toProto)
                    .toList();
            results.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (DataAccessException exception) {
            databaseError(responseObserver, exception);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        for (long eventId : request.getEventIdsList()) {
            if (!validPositive(eventId, "eventId", responseObserver)) {
                return;
            }
        }
        try {
            if (request.getEventIdsCount() == 0) {
                responseObserver.onCompleted();
                return;
            }
            Map<Long, Double> sums = interactionRepository
                    .sumWeightsByEventIds(request.getEventIdsList().stream().distinct().toList())
                    .stream()
                    .collect(Collectors.toMap(EventWeightSumView::getEventId,
                            EventWeightSumView::getWeightSum));
            List<RecommendedEventProto> results = new ArrayList<>(request.getEventIdsCount());
            for (long eventId : request.getEventIdsList()) {
                results.add(RecommendedEventProto.newBuilder()
                        .setEventId(eventId)
                        .setScore(sums.getOrDefault(eventId, 0.0))
                        .build());
            }
            results.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (DataAccessException exception) {
            databaseError(responseObserver, exception);
        }
    }

    private RecommendedEventProto toProto(SimilarEventView source) {
        return RecommendedEventProto.newBuilder()
                .setEventId(source.getEventId())
                .setScore(source.getScore())
                .build();
    }

    List<RecommendedEventProto> predict(List<CandidateNeighborView> neighbors, int maxResults) {
        Map<Long, PredictionAccumulator> predictions = new LinkedHashMap<>();
        for (CandidateNeighborView neighbor : neighbors) {
            if (neighbor.getCandidateId().equals(neighbor.getNeighborId()) || neighbor.getSimilarity() <= 0.0) {
                continue;
            }
            predictions.computeIfAbsent(neighbor.getCandidateId(), ignored -> new PredictionAccumulator())
                    .add(neighbor.getUserWeight(), neighbor.getSimilarity());
        }
        return predictions.entrySet().stream()
                .filter(entry -> entry.getValue().similaritySum > 0.0)
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue().weightedSum / entry.getValue().similaritySum)
                        .build())
                .sorted((left, right) -> {
                    int scoreComparison = Double.compare(right.getScore(), left.getScore());
                    return scoreComparison != 0
                            ? scoreComparison
                            : Long.compare(left.getEventId(), right.getEventId());
                })
                .limit(maxResults)
                .toList();
    }

    private static class PredictionAccumulator {
        private double weightedSum;
        private double similaritySum;

        void add(double userWeight, double similarity) {
            weightedSum += userWeight * similarity;
            similaritySum += similarity;
        }
    }

    private boolean validPositive(long value, String field,
                                  StreamObserver<RecommendedEventProto> responseObserver) {
        if (value > 0) {
            return true;
        }
        responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription(field + " must be positive")
                .asRuntimeException());
        return false;
    }

    private void databaseError(StreamObserver<RecommendedEventProto> responseObserver,
                               DataAccessException exception) {
        log.error("Failed to read recommendation data", exception);
        responseObserver.onError(Status.UNAVAILABLE
                .withDescription("Recommendation storage is unavailable")
                .withCause(exception)
                .asRuntimeException());
    }
}
