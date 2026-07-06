package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.events.model.EventState;
import ru.practicum.ewm.request.model.StatusRequest;
import ru.practicum.ewm.error.ConflictException;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.events.repository.EventRepository;
import ru.practicum.ewm.request.dto.RequestDto;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public RequestDto addUserRequest(long userId, long eventId) {
        log.info("Save request");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id: %s was not found", userId)));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id: %s was not found", eventId)));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot request participation in own event");
        }

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Cannot participate in unpublished event");
        }

        boolean exists = requestRepository
                .existsByRequesterIdAndEventId(userId, eventId);

        if (exists) {
            throw new ConflictException("Participation request already exists");
        }

        if (event.getParticipantLimit() > 0) {
            long confirmed = requestRepository.countByEventIdAndStatus(eventId, StatusRequest.CONFIRMED);
            if (confirmed >= event.getParticipantLimit()) {
                throw new ConflictException("Participant limit has been reached");
            }
        }

        StatusRequest statusRequest;

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            statusRequest = StatusRequest.CONFIRMED;
        } else {
            statusRequest = StatusRequest.PENDING;
        }

        Request request = Request.builder()
                .requester(user)
                .status(statusRequest)
                .created(LocalDateTime.now())
                .event(event)
                .build();

        Request saved = Objects.requireNonNull(requestRepository.save(request));
        RequestDto result = RequestMapper.toRequestDto(saved);

        if (statusRequest == StatusRequest.CONFIRMED) {
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        }

        return result;
    }

    @Override
    @Transactional
    public RequestDto cancelRequest(long requesterId, long requestId) {
        Request request = requestRepository.findById(requestId).orElseThrow(
                () -> new NotFoundException(String.format("Request with id: %s was not found", requestId)));

        if (!request.getRequester().getId().equals(requesterId)) {
            throw new NotFoundException(String.format("Requester with id: %s was not found", requesterId));
        }

        boolean wasConfirmed = request.getStatus() == StatusRequest.CONFIRMED;
        request.setStatus(StatusRequest.CANCELED);

        if (wasConfirmed) {
            Event event = request.getEvent();
            event.setConfirmedRequests(Math.max(0, event.getConfirmedRequests() - 1));
            eventRepository.save(event);
        }

        Request updated = requestRepository.save(request);

        return RequestMapper.toRequestDto(updated);
    }

    @Override
    public List<RequestDto> getUserRequests(long requesterId) {
        if (!userRepository.existsById(requesterId)) {
            throw new NotFoundException(String.format("User with id: %s was not found", requesterId));
        }

        return requestRepository.findAllByRequesterId(requesterId).stream()
                .map(RequestMapper::toRequestDto)
                .toList();
    }
}
