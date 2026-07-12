package ru.practicum.ewm.events.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.events.interaction.EventStatsFeignClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventPublicServiceImpl implements EventPublicService {
    private final EventRepository eventRepository;
    private final EventStatsFeignClient statsClient;
    private final EventEnricher eventEnricher;

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable,
                                               EventSort sort, int from, int size, HttpServletRequest httpRequest) {
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

        saveHit(httpRequest);

        Map<Long, Long> viewsMap = getViewsMap(events);

        List<EventShortDto> result = events.stream()
                .map(e -> {
                    EventShortDto dto = EventMapper.toEventShortDto(e);
                    dto.setViews(viewsMap.getOrDefault(e.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());

        if (sort == EventSort.VIEWS) {
            result.sort(Comparator.comparingLong(EventShortDto::getViews).reversed());
        }

        return result;
    }

    @Override
    public EventFullDto getPublicEventById(Long eventId, HttpServletRequest httpRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        eventEnricher.enrich(event);

        saveHit(httpRequest);

        long views = 0L;
        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    event.getPublishedOn(),
                    LocalDateTime.now().plusDays(3),
                    List.of("/events/" + eventId),
                    true);
            views = stats.isEmpty() ? 0L : stats.getFirst().getHits();
        } catch (Exception e) {
            log.warn("Failed to fetch view stats for event id={}: {}", eventId, e.getMessage(), e);
        }

        EventFullDto dto = EventMapper.toEventFullDto(event);
        dto.setViews(views);
        return dto;
    }

    private void saveHit(HttpServletRequest request) {
        try {
            statsClient.saveHit(new EndpointHitDto(
                    null,
                    "event-service",
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ));
        } catch (Exception e) {
            log.warn("Failed to save hit for uri={} ip={}: {}", request.getRequestURI(), request.getRemoteAddr(),
                    e.getMessage(), e);
        }
    }

    private Map<Long, Long> getViewsMap(List<Event> events) {
        if (events.isEmpty()) return Map.of();
        try {
            List<String> uris = events.stream()
                    .map(e -> "/events/" + e.getId())
                    .toList();
            List<ViewStatsDto> stats = statsClient.getStats(
                    LocalDateTime.now().minusMonths(2),
                    LocalDateTime.now().plusMonths(2),
                    uris,
                    true
            );
            return stats.stream()
                    .collect(Collectors.toMap(
                            s -> Long.parseLong(s.getUri().replace("/events/", "")),
                            ViewStatsDto::getHits
                    ));
        } catch (Exception e) {
            log.warn("Failed to fetch view stats for {} uris: {}", events.size(), e.getMessage(), e);
            return Map.of();
        }
    }
}
