package ru.practicum.ewm.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.stats.client.RecommendationsClient;
import ru.practicum.ewm.stats.client.StatsClientException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
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
        try {
            return recommendationsClient.getInteractionsCount(eventIds);
        } catch (StatsClientException exception) {
            log.warn("Analyzer is unavailable while loading ratings for {} events; using zero ratings: {}",
                    eventIds.size(), exception.getMessage());
            return Map.of();
        }
    }

    public double getRating(Event event) {
        return getRatings(List.of(event)).getOrDefault(event.getId(), 0.0);
    }
}
