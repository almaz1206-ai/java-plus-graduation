package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.practicum.ewm.events.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.events.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.model.StatusRequest;
import ru.practicum.ewm.error.ConflictException;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.ewm.request.dto.RequestDto;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.interaction.event.EventContract;
import ru.practicum.interaction.event.EventParticipationResponse;
import ru.practicum.interaction.event.EventState;
import ru.practicum.interaction.user.UserContract;
import ru.practicum.ewm.stats.client.ActionType;
import ru.practicum.ewm.stats.client.CollectorClient;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserContract userContract;
    private final EventContract eventContract;
    private final CollectorClient collectorClient;
    private final Clock clock;

    @Override
    @Transactional
    public RequestDto addUserRequest(long userId, long eventId) {
        log.info("Save request");

        if (!userContract.exists(userId).exists()) {
            throw new NotFoundException(String.format("User with id: %s was not found", userId));
        }
        EventParticipationResponse event = eventContract.getParticipationDetails(eventId);

        if (event.initiatorId().equals(userId)) {
            throw new ConflictException("Initiator cannot request participation in own event");
        }

        if (event.state() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in unpublished event");
        }

        boolean exists = requestRepository
                .existsByRequesterIdAndEventId(userId, eventId);

        if (exists) {
            throw new ConflictException("Participation request already exists");
        }

        if (event.participantLimit() > 0) {
            long confirmed = requestRepository.countByEventIdAndStatus(eventId, StatusRequest.CONFIRMED);
            if (confirmed >= event.participantLimit()) {
                throw new ConflictException("Participant limit has been reached");
            }
        }

        StatusRequest statusRequest;

        if (!event.requestModeration() || event.participantLimit() == 0) {
            statusRequest = StatusRequest.CONFIRMED;
        } else {
            statusRequest = StatusRequest.PENDING;
        }

        Request request = Request.builder()
                .requesterId(userId)
                .status(statusRequest)
                .created(LocalDateTime.now())
                .eventId(eventId)
                .build();

        Request saved = Objects.requireNonNull(requestRepository.save(request));
        RequestDto result = RequestMapper.toRequestDto(saved);

        if (statusRequest == StatusRequest.CONFIRMED) {
            eventContract.changeConfirmedRequests(eventId, 1);
        }

        sendRegisterAfterCommit(userId, eventId, Instant.now(clock));
        return result;
    }

    @Override
    @Transactional
    public RequestDto cancelRequest(long requesterId, long requestId) {
        Request request = requestRepository.findById(requestId).orElseThrow(
                () -> new NotFoundException(String.format("Request with id: %s was not found", requestId)));

        if (!request.getRequesterId().equals(requesterId)) {
            throw new NotFoundException(String.format("Requester with id: %s was not found", requesterId));
        }

        boolean wasConfirmed = request.getStatus() == StatusRequest.CONFIRMED;
        request.setStatus(StatusRequest.CANCELED);

        if (wasConfirmed) {
            eventContract.changeConfirmedRequests(request.getEventId(), -1);
        }

        Request updated = requestRepository.save(request);

        return RequestMapper.toRequestDto(updated);
    }

    @Override
    public List<RequestDto> getUserRequests(long requesterId) {
        if (!userContract.exists(requesterId).exists()) {
            throw new NotFoundException(String.format("User with id: %s was not found", requesterId));
        }

        return requestRepository.findAllByRequesterId(requesterId).stream()
                .map(RequestMapper::toRequestDto)
                .toList();
    }

    @Override
    public List<RequestDto> getEventRequests(Long userId, Long eventId) {
        getOwnedEvent(userId, eventId);
        return requestRepository.findAllByEventId(eventId).stream()
                .map(RequestMapper::toRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequest update) {
        EventParticipationResponse event = getOwnedEvent(userId, eventId);
        if (event.participantLimit() == 0 || !event.requestModeration()) {
            return new EventRequestStatusUpdateResult(List.of(), List.of());
        }

        List<Request> requests = requestRepository.findAllByIdInAndEventId(update.getRequestIds(), eventId);
        if (requests.size() != update.getRequestIds().size()) {
            throw new NotFoundException("One or more requests not found for event id=" + eventId);
        }
        requests.forEach(request -> {
            if (request.getStatus() != StatusRequest.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
        });

        if (update.getStatus() == StatusRequest.REJECTED) {
            requests.forEach(request -> request.setStatus(StatusRequest.REJECTED));
            requestRepository.saveAll(requests);
            return result(List.of(), requests);
        }

        int confirmedCount = (int) requestRepository.countByEventIdAndStatus(eventId, StatusRequest.CONFIRMED);
        if (confirmedCount >= event.participantLimit()
                || confirmedCount + requests.size() > event.participantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }
        requests.forEach(request -> request.setStatus(StatusRequest.CONFIRMED));
        int updatedCount = eventContract.changeConfirmedRequests(eventId, requests.size());

        List<Request> rejected = new ArrayList<>();
        if (updatedCount >= event.participantLimit()) {
            requestRepository.findAllByEventId(eventId).stream()
                    .filter(request -> request.getStatus() == StatusRequest.PENDING)
                    .forEach(request -> {
                        request.setStatus(StatusRequest.REJECTED);
                        rejected.add(request);
                    });
        }
        List<Request> changed = new ArrayList<>(requests);
        changed.addAll(rejected);
        requestRepository.saveAll(changed);
        return result(requests, rejected);
    }

    private EventParticipationResponse getOwnedEvent(Long userId, Long eventId) {
        EventParticipationResponse event = eventContract.getParticipationDetails(eventId);
        if (!event.initiatorId().equals(userId)) {
            throw new NotFoundException("Event with id: " + eventId + " was not found");
        }
        return event;
    }

    private EventRequestStatusUpdateResult result(List<Request> confirmed, List<Request> rejected) {
        return new EventRequestStatusUpdateResult(
                confirmed.stream().map(RequestMapper::toRequestDto).toList(),
                rejected.stream().map(RequestMapper::toRequestDto).toList());
    }

    private void sendRegisterAfterCommit(long userId, long eventId, Instant timestamp) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            collectorClient.collectUserAction(userId, eventId, ActionType.REGISTER, timestamp);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                collectorClient.collectUserAction(userId, eventId, ActionType.REGISTER, timestamp);
            }
        });
    }
}
