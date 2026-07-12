package ru.practicum.ewm.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction.event.CategoryContract;
import ru.practicum.interaction.event.CategoryResponse;
import ru.practicum.ewm.error.BadRequestException;
import ru.practicum.ewm.error.ConflictException;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.ewm.events.dto.*;
import ru.practicum.ewm.events.mapper.EventMapper;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.events.model.EventState;
import ru.practicum.ewm.events.repository.EventRepository;
import ru.practicum.interaction.user.UserContract;
import ru.practicum.interaction.user.UserResponse;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventPrivateServiceImpl implements EventPrivateService {
    @Value("${ewm.event.user-min-hours:2}")
    private long userMinHours;
    private final EventRepository eventRepository;
    private final UserContract userContract;
    private final CategoryContract categoryContract;
    private final EventEnricher eventEnricher;

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        Page<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        eventEnricher.enrich(events.getContent());
        return events.stream()
                .map(EventMapper::toEventShortDto)
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {
        validateEventDate(newEventDto.getEventDate());

        UserResponse initiator = userContract.getById(userId);
        CategoryResponse category = categoryContract.getById(newEventDto.getCategoryId());

        Event event = new Event();
        event.setTitle(newEventDto.getTitle());
        event.setAnnotation(newEventDto.getAnnotation());
        event.setDescription(newEventDto.getDescription());
        event.setInitiatorId(initiator.id());
        event.setInitiatorName(initiator.name());
        event.setCategoryId(category.id());
        event.setCategoryName(category.name());
        event.setPaid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false);
        event.setParticipantLimit(newEventDto.getParticipantLimit() != null ? newEventDto.getParticipantLimit() : 0);
        event.setRequestModeration(newEventDto.getRequestModeration() != null ? newEventDto.getRequestModeration() : true);
        event.setEventDate(newEventDto.getEventDate());
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());
        event.setConfirmedRequests(0);
        event.setViews(0L);
        if (newEventDto.getLocation() != null) {
            ru.practicum.ewm.events.model.Location loc = new ru.practicum.ewm.events.model.Location();
            loc.setLat(newEventDto.getLocation().getLat());
            loc.setLon(newEventDto.getLocation().getLon());
            event.setLocation(loc);
        }

        return EventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        Event event = eventRepository
                .findByIdAndInitiatorId(eventId, userId).orElseThrow(() ->
                        new NotFoundException(String.format("Event with id: %s was not found", eventId)));

        eventEnricher.enrich(event);
        return EventMapper.toEventFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        Event event = eventRepository
                .findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() ->
                        new NotFoundException(String.format("Event with id: %s was not found", eventId)));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (request.getEventDate() != null) {
            validateEventDate(request.getEventDate());
        }

        if (request.getCategoryId() != null) {
            CategoryResponse category = categoryContract.getById(request.getCategoryId());
            event.setCategoryId(category.id());
            event.setCategoryName(category.name());
        }

        EventMapper.updateEventFromDto(request, event);
        if (request.getLocation() != null) {
            ru.practicum.ewm.events.model.Location loc = new ru.practicum.ewm.events.model.Location();
            loc.setLat(request.getLocation().getLat());
            loc.setLon(request.getLocation().getLon());
            event.setLocation(loc);
        }

        Event saved = eventRepository.save(event);
        eventEnricher.enrich(saved);
        return EventMapper.toEventFullDto(saved);
    }

    private void validateEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(userMinHours))) {
            throw new BadRequestException("Event date must be at least 2 hours in the future");
        }
    }
}
