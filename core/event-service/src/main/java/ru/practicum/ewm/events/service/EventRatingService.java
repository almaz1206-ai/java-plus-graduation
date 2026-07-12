package ru.practicum.ewm.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.stats.client.RecommendationsClient;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventRatingService {
    private final RecommendationsClient recommendationsClient;

    public Map<Long, Double> getRatings(Collection<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .distinct()
                .toList();
        return recommendationsClient.getInteractionsCount(eventIds);
    }

    public double getRating(Event event) {
        return getRatings(List.of(event)).getOrDefault(event.getId(), 0.0);
    }
}
