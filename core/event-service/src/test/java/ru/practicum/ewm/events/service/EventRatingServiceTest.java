package ru.practicum.ewm.events.service;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.stats.client.RecommendationsClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EventRatingServiceTest {
    private final RecommendationsClient client = mock(RecommendationsClient.class);
    private final EventRatingService service = new EventRatingService(client);

    @Test
    void requestsAllRatingsInOneBulkCallAndPreservesMapping() {
        List<Event> events = List.of(Event.builder().id(2L).build(), Event.builder().id(1L).build());
        when(client.getInteractionsCount(List.of(2L, 1L))).thenReturn(Map.of(1L, 0.4, 2L, 1.8));

        Map<Long, Double> result = service.getRatings(events);

        assertThat(result).containsEntry(1L, 0.4).containsEntry(2L, 1.8);
        verify(client).getInteractionsCount(List.of(2L, 1L));
    }

    @Test
    void doesNotCallAnalyzerForEmptyCollection() {
        assertThat(service.getRatings(List.of())).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    void returnsZeroWhenAnalyzerHasNoRating() {
        Event event = Event.builder().id(5L).build();
        when(client.getInteractionsCount(List.of(5L))).thenReturn(Map.of());

        assertThat(service.getRating(event)).isZero();
    }
}
