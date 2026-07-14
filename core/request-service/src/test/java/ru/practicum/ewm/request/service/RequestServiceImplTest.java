package ru.practicum.ewm.request.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.error.ConflictException;
import ru.practicum.ewm.events.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.model.*;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.interaction.common.ExistenceResponse;
import ru.practicum.interaction.event.*;
import ru.practicum.interaction.user.UserContract;
import ru.practicum.ewm.stats.client.ActionType;
import ru.practicum.ewm.stats.client.CollectorClient;
import ru.practicum.ewm.stats.client.StatsClientException;
import io.grpc.Status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {
    @Mock
    RequestRepository repository;
    @Mock
    UserContract users;
    @Mock
    EventContract events;
    @Mock
    CollectorClient collectorClient;
    Clock clock = Clock.fixed(Instant.parse("2026-07-12T12:00:00Z"), ZoneOffset.UTC);
    RequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RequestServiceImpl(repository, users, events, collectorClient, clock);
    }

    @Test
    void createsRequest() {
        validUser();
        when(events.getParticipationDetails(2L)).thenReturn(event(3L, EventState.PUBLISHED, 10, true));
        when(repository.save(any(Request.class))).thenAnswer(i -> {
            Request r = i.getArgument(0);
            r.setId(4L);
            return r;
        });
        assertThat(service.addUserRequest(1L, 2L).getStatus()).isEqualTo(StatusRequest.PENDING);
        verify(collectorClient).collectUserAction(
                1L, 2L, ActionType.REGISTER, Instant.parse("2026-07-12T12:00:00Z"));
    }

    @Test
    void rejectsDuplicateRequest() {
        validUser();
        when(events.getParticipationDetails(2L)).thenReturn(event(3L, EventState.PUBLISHED, 10, true));
        when(repository.existsByRequesterIdAndEventId(1L, 2L)).thenReturn(true);
        assertThatThrownBy(() -> service.addUserRequest(1L, 2L)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(collectorClient);
    }

    @Test
    void rejectsInitiatorRequest() {
        validUser();
        when(events.getParticipationDetails(2L)).thenReturn(event(1L, EventState.PUBLISHED, 10, true));
        assertThatThrownBy(() -> service.addUserRequest(1L, 2L)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(collectorClient);
    }

    @Test
    void rejectsUnpublishedEvent() {
        validUser();
        when(events.getParticipationDetails(2L)).thenReturn(event(3L, EventState.PENDING, 10, true));
        assertThatThrownBy(() -> service.addUserRequest(1L, 2L)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(collectorClient);
    }

    @Test
    void doesNotRegisterWhenParticipantLimitIsReached() {
        validUser();
        when(events.getParticipationDetails(2L)).thenReturn(event(3L, EventState.PUBLISHED, 1, true));
        when(repository.countByEventIdAndStatus(2L, StatusRequest.CONFIRMED)).thenReturn(1L);

        assertThatThrownBy(() -> service.addUserRequest(1L, 2L)).isInstanceOf(ConflictException.class);

        verifyNoInteractions(collectorClient);
    }

    @Test
    void cancelsRequest() {
        Request request = request(4L, 1L, 2L, StatusRequest.PENDING);
        when(repository.findById(4L)).thenReturn(Optional.of(request));
        when(repository.save(request)).thenReturn(request);
        assertThat(service.cancelRequest(1L, 4L).getStatus()).isEqualTo(StatusRequest.CANCELED);
    }

    @Test
    void confirmsRequestsInBatch() {
        List<Request> requests = List.of(request(4L, 5L, 2L, StatusRequest.PENDING), request(6L, 7L, 2L, StatusRequest.PENDING));
        ownerEvent();
        when(repository.findAllByIdInAndEventId(List.of(4L, 6L), 2L)).thenReturn(requests);
        when(repository.countByEventIdAndStatus(2L, StatusRequest.CONFIRMED)).thenReturn(1L);
        when(events.changeConfirmedRequests(2L, 2)).thenReturn(3);
        var result = service.changeRequestStatus(3L, 2L, new EventRequestStatusUpdateRequest(List.of(4L, 6L), StatusRequest.CONFIRMED));
        assertThat(result.getConfirmedRequests()).hasSize(2);
        verify(events, times(1)).getParticipationDetails(2L);
    }

    @Test
    void rejectsRequestsInBatch() {
        List<Request> requests = List.of(request(4L, 5L, 2L, StatusRequest.PENDING), request(6L, 7L, 2L, StatusRequest.PENDING));
        ownerEvent();
        when(repository.findAllByIdInAndEventId(List.of(4L, 6L), 2L)).thenReturn(requests);
        assertThat(service.changeRequestStatus(3L, 2L, new EventRequestStatusUpdateRequest(List.of(4L, 6L), StatusRequest.REJECTED)).getRejectedRequests()).hasSize(2);
    }

    @Test
    void rejectsWhenLimitReached() {
        ownerEvent();
        when(repository.findAllByIdInAndEventId(List.of(4L), 2L)).thenReturn(List.of(request(4L, 5L, 2L, StatusRequest.PENDING)));
        when(repository.countByEventIdAndStatus(2L, StatusRequest.CONFIRMED)).thenReturn(10L);
        assertThatThrownBy(() -> service.changeRequestStatus(3L, 2L, new EventRequestStatusUpdateRequest(List.of(4L), StatusRequest.CONFIRMED)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void autoConfirmsWhenModerationDisabled() {
        validUser();
        when(events.getParticipationDetails(2L)).thenReturn(event(3L, EventState.PUBLISHED, 10, false));
        when(repository.save(any(Request.class))).thenAnswer(i -> {
            Request r = i.getArgument(0);
            r.setId(4L);
            return r;
        });
        assertThat(service.addUserRequest(1L, 2L).getStatus()).isEqualTo(StatusRequest.CONFIRMED);
        verify(events).changeConfirmedRequests(2L, 1);
        verify(collectorClient).collectUserAction(
                1L, 2L, ActionType.REGISTER, Instant.parse("2026-07-12T12:00:00Z"));
    }

    @Test
    void doesNotRegisterWhenSavingRequestFails() {
        validUser();
        when(events.getParticipationDetails(2L)).thenReturn(event(3L, EventState.PUBLISHED, 10, true));
        when(repository.save(any(Request.class))).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> service.addUserRequest(1L, 2L)).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(collectorClient);
    }

    @Test
    void propagatesCollectorFailureAfterSavingRequest() {
        validUser();
        when(events.getParticipationDetails(2L)).thenReturn(event(3L, EventState.PUBLISHED, 10, true));
        when(repository.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));
        StatsClientException failure = new StatsClientException(
                "collectUserAction", Status.UNAVAILABLE.asRuntimeException());
        doThrow(failure).when(collectorClient).collectUserAction(
                1L, 2L, ActionType.REGISTER, Instant.parse("2026-07-12T12:00:00Z"));

        assertThatThrownBy(() -> service.addUserRequest(1L, 2L)).isSameAs(failure);

        verify(repository).save(any(Request.class));
    }

    private void validUser() {
        when(users.exists(1L)).thenReturn(new ExistenceResponse(1L, true));
    }

    private void ownerEvent() {
        when(events.getParticipationDetails(2L)).thenReturn(event(3L, EventState.PUBLISHED, 10, true));
    }

    private EventParticipationResponse event(Long owner, EventState state, int limit, boolean moderation) {
        return new EventParticipationResponse(2L, owner, state, limit, moderation);
    }

    private Request request(Long id, Long user, Long event, StatusRequest status) {
        return Request.builder().id(id).requesterId(user).eventId(event).status(status).created(LocalDateTime.now()).build();
    }
}
