package ru.practicum.ewm.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.error.BadRequestException;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.ewm.events.dto.EventFullDto;
import ru.practicum.ewm.events.dto.EventShortDto;
import ru.practicum.ewm.events.mapper.EventMapper;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.events.model.EventSort;
import ru.practicum.ewm.events.model.EventState;
import ru.practicum.ewm.events.repository.EventRepository;
import ru.practicum.ewm.events.repository.EventSpecification;
import ru.practicum.ewm.stats.client.ActionType;
import ru.practicum.ewm.stats.client.CollectorClient;
import ru.practicum.ewm.stats.client.RecommendedEvent;
import ru.practicum.ewm.stats.client.RecommendationsClient;
import ru.practicum.interaction.request.RequestContract;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventPublicServiceImpl implements EventPublicService {
    @Value("${ewm.event.recommendations.default-limit:10}")
    private int defaultRecommendationsLimit;
    @Value("${ewm.event.recommendations.max-limit:100}")
    private int maxRecommendationsLimit;
    private final EventRepository eventRepository;
    private final EventEnricher eventEnricher;
    private final EventRatingService eventRatingService;
    private final CollectorClient collectorClient;
    private final RecommendationsClient recommendationsClient;
    private final RequestContract requestContract;
    private final Clock clock;

    @Override
    public void likeEvent(Long eventId, long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        boolean confirmed = requestContract.hasConfirmedParticipation(userId, eventId).exists();
        if (!confirmed) {
            throw new BadRequestException("Only a confirmed participant can like an event");
        }
        Instant timestamp = Instant.now(clock);
        if (event.getEventDate().isAfter(LocalDateTime.now(clock))) {
            throw new BadRequestException("An event can be liked only after it has taken place");
        }
        collectorClient.collectUserAction(userId, eventId, ActionType.LIKE, timestamp);
    }

    @Override
    public List<EventShortDto> getRecommendations(long userId, Integer size) {
        int limit = size == null ? defaultRecommendationsLimit : size;
        if (limit <= 0 || limit > maxRecommendationsLimit) {
            throw new BadRequestException("Recommendation size must be between 1 and " + maxRecommendationsLimit);
        }

        List<RecommendedEvent> recommendations;
        try (var stream = recommendationsClient.getRecommendationsForUser(userId, limit)) {
            recommendations = stream.limit(limit).toList();
        }
        if (recommendations.isEmpty()) {
            return List.of();
        }

        Map<Long, Double> scores = recommendations.stream().collect(Collectors.toMap(
                RecommendedEvent::eventId,
                RecommendedEvent::score,
                (first, ignored) -> first,
                LinkedHashMap::new));
        List<Long> recommendedIds = List.copyOf(scores.keySet());
        Map<Long, Event> eventsById = eventRepository.findAllById(recommendedIds).stream()
                .filter(this::isPubliclyAvailable)
                .collect(Collectors.toMap(Event::getId, Function.identity()));
        List<Event> orderedEvents = scores.keySet().stream()
                .map(eventsById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        eventEnricher.enrich(orderedEvents);
        return orderedEvents.stream()
                .map(event -> EventMapper.toEventShortDto(event, scores.get(event.getId())))
                .toList();
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable,
                                               EventSort sort, int from, int size) {
        int page = from / size;

        Pageable pageable = (sort == EventSort.EVENT_DATE)
                ? PageRequest.of(page, size, Sort.by("eventDate").ascending())
                : PageRequest.of(page, size);

        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new BadRequestException("rangeEnd must not be before rangeStart");
        }

        LocalDateTime effectiveStart = rangeStart != null ? rangeStart : LocalDateTime.now();

        Specification<Event> spec = Specification
                .where(EventSpecification.hasState(EventState.PUBLISHED))
                .and(EventSpecification.hasText(text))
                .and(EventSpecification.hasCategories(categories))
                .and(EventSpecification.hasPaid(paid))
                .and(EventSpecification.eventDateAfter(effectiveStart))
                .and(EventSpecification.eventDateBefore(rangeEnd))
                .and(EventSpecification.isAvailable(onlyAvailable));

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();
        eventEnricher.enrich(events);

        Map<Long, Double> ratings = eventRatingService.getRatings(events);

        List<EventShortDto> result = events.stream()
                .map(e -> {
                    return EventMapper.toEventShortDto(e, ratings.getOrDefault(e.getId(), 0.0));
                })
                .collect(Collectors.toList());

        if (sort == EventSort.VIEWS) {
            result.sort(Comparator.comparingDouble(EventShortDto::getRating).reversed());
        }

        return result;
    }

    @Override
    public EventFullDto getPublicEventById(Long eventId, long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        eventEnricher.enrich(event);

        EventFullDto result = EventMapper.toEventFullDto(event, eventRatingService.getRating(event));
        collectorClient.collectUserAction(userId, eventId, ActionType.VIEW, Instant.now(clock));
        return result;
    }

    private boolean isPubliclyAvailable(Event event) {
        return event.getState() == EventState.PUBLISHED
                && !event.getEventDate().isBefore(LocalDateTime.now(clock));
    }
}
