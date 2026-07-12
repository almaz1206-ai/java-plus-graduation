package ru.practicum.ewm.events.interaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.events.repository.EventRepository;
import ru.practicum.ewm.events.service.EventEnricher;
import ru.practicum.ewm.events.service.EventRatingService;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.event.CategoryResponse;
import ru.practicum.interaction.event.EventContract;
import ru.practicum.interaction.event.EventParticipationResponse;
import ru.practicum.interaction.event.EventState;
import ru.practicum.interaction.event.EventSummaryResponse;
import ru.practicum.interaction.event.EventsResponse;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventContractAdapter implements EventContract {
    private final EventRepository eventRepository;
    private final EventEnricher enricher;
    private final EventRatingService ratingService;

    @Override
    public EventSummaryResponse getById(Long eventId) {
        Event event = event(eventId);
        enricher.enrich(event);
        return map(event, ratingService.getRating(event));
    }

    @Override
    public EventsResponse getByIds(IdsRequest request) {
        List<Event> events = eventRepository.findAllById(request.ids());
        enricher.enrich(events);
        var ratings = ratingService.getRatings(events);
        return new EventsResponse(events.stream()
                .map(event -> map(event, ratings.getOrDefault(event.getId(), 0.0))).toList());
    }

    @Override
    public EventParticipationResponse getParticipationDetails(Long eventId) {
        Event event = event(eventId);
        return new EventParticipationResponse(eventId, event.getInitiatorId(),
                EventState.valueOf(event.getState().name()), event.getParticipantLimit(), event.getRequestModeration());
    }

    @Override
    public boolean isInitiator(Long eventId, Long userId) {
        return event(eventId).getInitiatorId().equals(userId);
    }

    @Override
    public boolean hasState(Long eventId, EventState state) {
        return event(eventId).getState().name().equals(state.name());
    }

    @Override
    public int getParticipantLimit(Long eventId) {
        return event(eventId).getParticipantLimit();
    }

    @Override
    @Transactional
    public int changeConfirmedRequests(Long eventId, int delta) {
        Event event = event(eventId);
        event.setConfirmedRequests(Math.max(0, event.getConfirmedRequests() + delta));
        return event.getConfirmedRequests();
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return eventRepository.existsByCategoryId(categoryId);
    }

    private Event event(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));
    }

    private EventSummaryResponse map(Event event, double rating) {
        return new EventSummaryResponse(event.getId(), event.getTitle(), event.getAnnotation(),
                event.getInitiatorId(), event.getInitiatorName(),
                new CategoryResponse(event.getCategoryId(), event.getCategoryName()), event.getPaid(),
                event.getEventDate(), event.getConfirmedRequests(), rating);
    }

}
