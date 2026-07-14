package ru.practicum.ewm.stats.analyzer.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.EventWeightSumView;
import ru.practicum.ewm.stats.analyzer.repository.CandidateNeighborView;
import ru.practicum.ewm.stats.analyzer.repository.SimilarEventView;
import ru.practicum.ewm.stats.analyzer.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.analyzer.config.RecommendationProperties;
import ru.practicum.stats.service.dashboard.InteractionsCountRequestProto;
import ru.practicum.stats.service.dashboard.RecommendedEventProto;
import ru.practicum.stats.service.dashboard.SimilarEventsRequestProto;
import ru.practicum.stats.service.dashboard.UserPredictionsRequestProto;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class RecommendationsGrpcServiceTest {

    @Mock
    private EventSimilarityRepository similarityRepository;
    @Mock
    private UserInteractionRepository interactionRepository;

    @Test
    void emptyDatabaseShouldCompleteEmptySimilarEventsStream() {
        when(similarityRepository.findUnseenSimilarEvents(
                eq(10L), eq(1L), any(Pageable.class))).thenReturn(List.of());
        RecordingObserver observer = new RecordingObserver();

        service().getSimilarEvents(similarRequest(10), observer);

        assertTrue(observer.values.isEmpty());
        assertTrue(observer.completed);
        assertNull(observer.error);
    }

    @Test
    void shouldStreamSortedRepositoryResultsAndApplyLimit() {
        SimilarEventView first = view(20L, 0.9);
        SimilarEventView second = view(30L, 0.8);
        when(similarityRepository.findUnseenSimilarEvents(
                eq(10L), eq(1L), any(Pageable.class)))
                .thenReturn(List.of(first, second));
        RecordingObserver observer = new RecordingObserver();

        service().getSimilarEvents(similarRequest(2), observer);

        assertEquals(List.of(20L, 30L), observer.values.stream()
                .map(RecommendedEventProto::getEventId).toList());
        assertTrue(observer.completed);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(similarityRepository).findUnseenSimilarEvents(
                eq(10L), eq(1L), pageable.capture());
        assertEquals(2, pageable.getValue().getPageSize());
    }

    @Test
    void interactionsCountShouldPreserveOrderDuplicatesAndZeroes() {
        EventWeightSumView weightSum = weightSum(10L, 1.2);
        when(interactionRepository.sumWeightsByEventIds(List.of(10L, 20L)))
                .thenReturn(List.of(weightSum));
        RecordingObserver observer = new RecordingObserver();
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addEventIds(10L).addEventIds(20L).addEventIds(10L).build();

        service().getInteractionsCount(request, observer);

        assertEquals(List.of(10L, 20L, 10L), observer.values.stream()
                .map(RecommendedEventProto::getEventId).toList());
        assertEquals(List.of(1.2, 0.0, 1.2), observer.values.stream()
                .map(RecommendedEventProto::getScore).toList());
        assertTrue(observer.completed);
        verify(interactionRepository).sumWeightsByEventIds(List.of(10L, 20L));
    }

    @Test
    void emptyInteractionsRequestShouldCompleteEmptyStream() {
        RecordingObserver observer = new RecordingObserver();

        service().getInteractionsCount(InteractionsCountRequestProto.getDefaultInstance(), observer);

        assertTrue(observer.values.isEmpty());
        assertTrue(observer.completed);
    }

    @Test
    void userWithoutHistoryShouldCompleteEmptyRecommendationsStream() {
        when(similarityRepository.findRecommendationNeighbors(1L, 50, 200, 2)).thenReturn(List.of());
        RecordingObserver observer = new RecordingObserver();
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(1L).setMaxResults(10).build();

        service().getRecommendationsForUser(request, observer);

        assertTrue(observer.values.isEmpty());
        assertTrue(observer.completed);
    }

    @Test
    void shouldPredictOneCandidateFromOneNeighbor() {
        CandidateNeighborView neighbor = neighbor(20L, 10L, 0.8, 0.5);

        List<RecommendedEventProto> result = service().predict(List.of(neighbor), 10);

        assertEquals(1, result.size());
        assertEquals(20L, result.getFirst().getEventId());
        assertEquals(0.8, result.getFirst().getScore(), 1.0e-9);
    }

    @Test
    void shouldCalculateWeightedPredictionFromSeveralNeighbors() {
        CandidateNeighborView first = neighbor(20L, 10L, 0.4, 0.9);
        CandidateNeighborView second = neighbor(20L, 11L, 1.0, 0.3);

        List<RecommendedEventProto> result = service().predict(List.of(first, second), 10);

        assertEquals((0.4 * 0.9 + 1.0 * 0.3) / 1.2, result.getFirst().getScore(), 1.0e-9);
    }

    @Test
    void shouldSortCandidatesLimitResultsAndUseEventIdForTies() {
        CandidateNeighborView high = neighbor(30L, 10L, 1.0, 0.5);
        CandidateNeighborView tieHigherId = neighbor(20L, 10L, 0.8, 0.5);
        CandidateNeighborView tieLowerId = neighbor(15L, 10L, 0.8, 0.5);

        List<RecommendedEventProto> result = service().predict(
                List.of(tieHigherId, high, tieLowerId), 2);

        assertEquals(List.of(30L, 15L), result.stream()
                .map(RecommendedEventProto::getEventId).toList());
    }

    @Test
    void shouldSkipZeroSimilarityAndSelfNeighbor() {
        CandidateNeighborView zero = neighbor(20L, 10L, 1.0, 0.0);
        CandidateNeighborView self = neighbor(30L, 30L, 1.0, 1.0);

        assertTrue(service().predict(List.of(zero, self), 10).isEmpty());
    }

    @Test
    void recommendationsShouldPassConfiguredHistoryCandidateAndNeighborLimitsToBulkQuery() {
        when(similarityRepository.findRecommendationNeighbors(1L, 50, 200, 2)).thenReturn(List.of());
        RecordingObserver observer = new RecordingObserver();

        service().getRecommendationsForUser(UserPredictionsRequestProto.newBuilder()
                .setUserId(1L).setMaxResults(3).build(), observer);

        verify(similarityRepository).findRecommendationNeighbors(1L, 50, 200, 2);
        assertTrue(observer.completed);
    }

    @Test
    void shouldRejectInvalidSimilarEventsArguments() {
        RecordingObserver observer = new RecordingObserver();

        service().getSimilarEvents(SimilarEventsRequestProto.newBuilder()
                .setEventId(0L).setUserId(1L).setMaxResults(10).build(), observer);

        assertEquals(Status.Code.INVALID_ARGUMENT, status(observer.error));
        assertTrue(!observer.completed);
        verifyNoInteractions(similarityRepository);
    }

    @Test
    void shouldRejectInvalidMaxResultsAndUserId() {
        RecordingObserver first = new RecordingObserver();
        RecordingObserver second = new RecordingObserver();

        service().getRecommendationsForUser(UserPredictionsRequestProto.newBuilder()
                .setUserId(-1L).setMaxResults(10).build(), first);
        service().getSimilarEvents(SimilarEventsRequestProto.newBuilder()
                .setEventId(1L).setUserId(1L).setMaxResults(0).build(), second);

        assertEquals(Status.Code.INVALID_ARGUMENT, status(first.error));
        assertEquals(Status.Code.INVALID_ARGUMENT, status(second.error));
    }

    @Test
    void shouldRejectInvalidEventInsideInteractionsListBeforeStreaming() {
        RecordingObserver observer = new RecordingObserver();
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addEventIds(10L).addEventIds(0L).build();

        service().getInteractionsCount(request, observer);

        assertTrue(observer.values.isEmpty());
        assertEquals(Status.Code.INVALID_ARGUMENT, status(observer.error));
        verifyNoInteractions(interactionRepository);
    }

    private RecommendationsGrpcService service() {
        return new RecommendationsGrpcService(similarityRepository, interactionRepository,
                new RecommendationProperties(50, 200, 2));
    }

    private SimilarEventsRequestProto similarRequest(int maxResults) {
        return SimilarEventsRequestProto.newBuilder()
                .setEventId(10L).setUserId(1L).setMaxResults(maxResults).build();
    }

    private SimilarEventView view(long eventId, double score) {
        SimilarEventView view = mock(SimilarEventView.class);
        when(view.getEventId()).thenReturn(eventId);
        when(view.getScore()).thenReturn(score);
        return view;
    }

    private EventWeightSumView weightSum(long eventId, double sum) {
        EventWeightSumView view = mock(EventWeightSumView.class);
        when(view.getEventId()).thenReturn(eventId);
        when(view.getWeightSum()).thenReturn(sum);
        return view;
    }

    private CandidateNeighborView neighbor(long candidateId, long neighborId,
                                           double userWeight, double similarity) {
        return new CandidateNeighborView() {
            @Override
            public Long getCandidateId() {
                return candidateId;
            }

            @Override
            public Long getNeighborId() {
                return neighborId;
            }

            @Override
            public Double getUserWeight() {
                return userWeight;
            }

            @Override
            public Double getSimilarity() {
                return similarity;
            }
        };
    }

    private Status.Code status(Throwable error) {
        return ((StatusRuntimeException) error).getStatus().getCode();
    }

    private static class RecordingObserver implements StreamObserver<RecommendedEventProto> {
        private final List<RecommendedEventProto> values = new ArrayList<>();
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(RecommendedEventProto value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable throwable) {
            error = throwable;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }
}
