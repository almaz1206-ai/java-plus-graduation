package ru.practicum.ewm.stats.client;

import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.stats.service.dashboard.InteractionsCountRequestProto;
import ru.practicum.stats.service.dashboard.RecommendationsControllerGrpc;
import ru.practicum.stats.service.dashboard.RecommendedEventProto;
import ru.practicum.stats.service.dashboard.SimilarEventsRequestProto;
import ru.practicum.stats.service.dashboard.UserPredictionsRequestProto;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationsClientTest {
    private final RecommendationsControllerGrpc.RecommendationsControllerBlockingStub stub = mock(
            RecommendationsControllerGrpc.RecommendationsControllerBlockingStub.class);
    private final RecommendationsClient client = new RecommendationsClient();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "client", stub);
    }

    @Test
    void shouldBuildUserRecommendationsRequestAndMapStream() {
        when(stub.getRecommendationsForUser(any())).thenReturn(List.of(event(20L, 0.8)).iterator());

        List<RecommendedEvent> result = client.getRecommendationsForUser(10L, 5).toList();

        ArgumentCaptor<UserPredictionsRequestProto> captor =
                ArgumentCaptor.forClass(UserPredictionsRequestProto.class);
        verify(stub).getRecommendationsForUser(captor.capture());
        assertEquals(10L, captor.getValue().getUserId());
        assertEquals(5, captor.getValue().getMaxResults());
        assertEquals(List.of(new RecommendedEvent(20L, 0.8)), result);
    }

    @Test
    void shouldBuildSimilarEventsRequest() {
        when(stub.getSimilarEvents(any())).thenReturn(List.of(event(30L, 0.7)).iterator());

        List<RecommendedEvent> result = client.getSimilarEvents(20L, 10L, 3).toList();

        ArgumentCaptor<SimilarEventsRequestProto> captor =
                ArgumentCaptor.forClass(SimilarEventsRequestProto.class);
        verify(stub).getSimilarEvents(captor.capture());
        assertEquals(20L, captor.getValue().getEventId());
        assertEquals(10L, captor.getValue().getUserId());
        assertEquals(3, captor.getValue().getMaxResults());
        assertEquals(List.of(new RecommendedEvent(30L, 0.7)), result);
    }

    @Test
    void shouldReturnEmptySequentialStream() {
        when(stub.getRecommendationsForUser(any())).thenReturn(Collections.emptyIterator());

        try (var result = client.getRecommendationsForUser(1L, 1)) {
            assertFalse(result.isParallel());
            assertEquals(0L, result.count());
        }
    }

    @Test
    void shouldPreserveOrderInInteractionsCount() {
        when(stub.getInteractionsCount(any())).thenReturn(List.of(
                event(3L, 1.2), event(1L, 0.0), event(3L, 1.5)).iterator());

        Map<Long, Double> result = client.getInteractionsCount(List.of(3L, 1L, 3L));

        ArgumentCaptor<InteractionsCountRequestProto> captor =
                ArgumentCaptor.forClass(InteractionsCountRequestProto.class);
        verify(stub).getInteractionsCount(captor.capture());
        assertEquals(List.of(3L, 1L, 3L), captor.getValue().getEventIdsList());
        Map<Long, Double> expected = new LinkedHashMap<>();
        expected.put(3L, 1.5);
        expected.put(1L, 0.0);
        assertEquals(expected, result);
    }

    @Test
    void shouldConvertImmediateGrpcFailure() {
        when(stub.getSimilarEvents(any())).thenThrow(Status.UNAVAILABLE.asRuntimeException());

        StatsClientException exception = assertThrows(StatsClientException.class,
                () -> client.getSimilarEvents(1L, 2L, 3));

        assertEquals(Status.Code.UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void shouldConvertFailureDuringStreamConsumption() {
        Iterator<RecommendedEventProto> iterator = mock(Iterator.class);
        when(iterator.hasNext()).thenThrow(Status.INTERNAL.asRuntimeException());
        when(stub.getRecommendationsForUser(any())).thenReturn(iterator);

        StatsClientException exception = assertThrows(StatsClientException.class,
                () -> client.getRecommendationsForUser(1L, 1).toList());

        assertEquals(Status.Code.INTERNAL, exception.getStatusCode());
    }

    private RecommendedEventProto event(long eventId, double score) {
        return RecommendedEventProto.newBuilder().setEventId(eventId).setScore(score).build();
    }
}
