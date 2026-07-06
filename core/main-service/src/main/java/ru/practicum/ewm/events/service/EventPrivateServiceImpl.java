package ru.practicum.ewm.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.categories.model.Category;
import ru.practicum.ewm.categories.repository.CategoryRepository;
import ru.practicum.ewm.error.BadRequestException;
import ru.practicum.ewm.error.ConflictException;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.ewm.events.dto.*;
import ru.practicum.ewm.events.mapper.EventMapper;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.events.model.EventState;
import ru.practicum.ewm.events.repository.EventRepository;
import ru.practicum.ewm.request.dto.RequestDto;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.model.StatusRequest;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventPrivateServiceImpl implements EventPrivateService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        Page<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        return events.stream()
                .map(EventMapper::toEventShortDto)
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {
        validateEventDate(newEventDto.getEventDate());

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Category category = categoryRepository.findById(newEventDto.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category with id=" + newEventDto.getCategoryId() + " was not found"));

        Event event = new Event();
        event.setTitle(newEventDto.getTitle());
        event.setAnnotation(newEventDto.getAnnotation());
        event.setDescription(newEventDto.getDescription());
        event.setInitiator(initiator);
        event.setCategory(category);
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
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + request.getCategoryId() + " was not found"));
            event.setCategory(category);
        }

        EventMapper.updateEventFromDto(request, event);
        if (request.getLocation() != null) {
            ru.practicum.ewm.events.model.Location loc = new ru.practicum.ewm.events.model.Location();
            loc.setLat(request.getLocation().getLat());
            loc.setLon(request.getLocation().getLon());
            event.setLocation(loc);
        }

        return EventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    public List<RequestDto> getEventRequests(Long userId, Long eventId) {
        Event event = eventRepository
                .findByIdAndInitiatorId(eventId, userId).orElseThrow(() ->
                        new NotFoundException(String.format("Event with id: %s was not found", eventId)));

        return requestRepository.findAllByEventId(event.getId()).stream()
                .map(RequestMapper::toRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId,
                                                              Long eventId,
                                                              EventRequestStatusUpdateRequest request) {
        Event event = getOwnedEvent(userId, eventId);

        if (isAutoConfirmationDisabled(event)) {
            return emptyResult();
        }

        List<Request> requests = requestRepository.findAllByIdInAndEventId(request.getRequestIds(), event.getId());

        if (requests.size() != request.getRequestIds().size()) {
            throw new NotFoundException("One or more requests not found for event id=" + eventId);
        }

        validateRequests(requests);

        if (request.getStatus() == StatusRequest.CONFIRMED) {
            return processConfirmRequests(event, requests);
        } else {
            return processRejectRequests(requests);
        }
    }

    private Event getOwnedEvent(Long userId, Long eventId) {
        return eventRepository
                .findByIdAndInitiatorId(eventId, userId).orElseThrow(() ->
                        new NotFoundException(String.format("Event with id: %s was not found", eventId)));
    }

    private boolean isAutoConfirmationDisabled(Event event) {
        return event.getParticipantLimit() == 0 || !event.getRequestModeration();
    }

    private EventRequestStatusUpdateResult emptyResult() {
        return new EventRequestStatusUpdateResult(List.of(), List.of());
    }

    private void validateRequests(List<Request> requests) {
        for (Request pr : requests) {
            if (pr.getStatus() != StatusRequest.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
        }
    }

    private EventRequestStatusUpdateResult processConfirmRequests(Event event, List<Request> requests) {
        int confirmedCount = event.getConfirmedRequests();
        int limit = event.getParticipantLimit();

        if (confirmedCount >= limit) {
            throw new ConflictException("The participant limit has been reached");
        }

        List<Request> confirmed = new ArrayList<>();
        List<Request> rejected = new ArrayList<>();

        for (Request pr : requests) {
            if (confirmedCount >= limit) {
                throw new ConflictException("The participant limit has been reached");
            }
            pr.setStatus(StatusRequest.CONFIRMED);
            confirmed.add(pr);
            confirmedCount++;
        }

        event.setConfirmedRequests(confirmedCount);

        rejectRemainingIfLimitReached(event, rejected);

        return buildResult(confirmed, rejected);
    }

    private EventRequestStatusUpdateResult processRejectRequests(List<Request> requests) {
        List<Request> rejected = new ArrayList<>();

        for (Request pr : requests) {
            pr.setStatus(StatusRequest.REJECTED);
            rejected.add(pr);
        }

        return buildResult(List.of(), rejected);
    }

    private void rejectRemainingIfLimitReached(Event event, List<Request> rejected) {
        if (event.getConfirmedRequests() < event.getParticipantLimit()) {
            return;
        }

        List<Request> pending = requestRepository.findAllByEventId(event.getId())
                .stream()
                .filter(r -> r.getStatus() == StatusRequest.PENDING)
                .toList();

        for (Request pr : pending) {
            pr.setStatus(StatusRequest.REJECTED);
            rejected.add(pr);
        }
    }

    private EventRequestStatusUpdateResult buildResult(List<Request> confirmed, List<Request> rejected) {
        return new EventRequestStatusUpdateResult(
                confirmed.stream().map(RequestMapper::toRequestDto).toList(),
                rejected.stream().map(RequestMapper::toRequestDto).toList()
        );
    }

    private void validateEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours in the future");
        }
    }
}
