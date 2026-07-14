package ru.practicum.ewm.events.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.events.dto.EventShortDto;
import ru.practicum.ewm.error.BadRequestException;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.events.model.EventState;
import ru.practicum.ewm.events.repository.EventRepository;
import ru.practicum.ewm.stats.client.ActionType;
import ru.practicum.ewm.stats.client.CollectorClient;
import ru.practicum.ewm.stats.client.RecommendedEvent;
import ru.practicum.ewm.stats.client.RecommendationsClient;
import ru.practicum.ewm.stats.client.StatsClientException;
import ru.practicum.interaction.request.ParticipationRequestExistsResponse;
import ru.practicum.interaction.request.RequestContract;
import io.grpc.Status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class EventPublicServiceImplTest {
    @Mock
    EventRepository repository;
    @Mock
    EventEnricher enricher;
    @Mock
    EventRatingService ratingService;
    @Mock
    CollectorClient collectorClient;
    @Mock
    RecommendationsClient recommendationsClient;
    @Mock
    RequestContract requestContract;
    Clock clock = Clock.fixed(Instant.parse("2026-07-12T12:00:00Z"), ZoneOffset.UTC);
    EventPublicServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventPublicServiceImpl(
                repository, enricher, ratingService, collectorClient, recommendationsClient, requestContract, clock);
        ReflectionTestUtils.setField(service, "defaultRecommendationsLimit", 2);
        ReflectionTestUtils.setField(service, "maxRecommendationsLimit", 5);
    }

    @Test
    void doesNotLikeMissingEvent() {
        when(repository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.likeEvent(50L, 10L))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(requestContract, collectorClient);
    }

    @Test
    void rejectsLikeWithoutConfirmedRequest() {
        Event event = occurredEvent(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(event));
        when(requestContract.hasConfirmedParticipation(10L, 5L))
                .thenReturn(new ParticipationRequestExistsResponse(10L, 5L, false));

        assertThatThrownBy(() -> service.likeEvent(5L, 10L))
                .isInstanceOf(BadRequestException.class);

        verify(collectorClient, never()).collectUserAction(anyLong(), anyLong(), any(), any());
    }

    @Test
    void rejectsLikeForFutureEventAfterConfirmedRequestCheck() {
        Event event = event(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(event));
        when(requestContract.hasConfirmedParticipation(10L, 5L))
                .thenReturn(new ParticipationRequestExistsResponse(10L, 5L, true));

        assertThatThrownBy(() -> service.likeEvent(5L, 10L))
                .isInstanceOf(BadRequestException.class);

        verify(requestContract).hasConfirmedParticipation(10L, 5L);
        verify(collectorClient, never()).collectUserAction(anyLong(), anyLong(), any(), any());
    }

    @Test
    void sendsLikeAfterAllChecksAndAllowsRepeatedCall() {
        Event event = occurredEvent(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(event));
        when(requestContract.hasConfirmedParticipation(10L, 5L))
                .thenReturn(new ParticipationRequestExistsResponse(10L, 5L, true));

        service.likeEvent(5L, 10L);
        service.likeEvent(5L, 10L);

        verify(collectorClient, times(2)).collectUserAction(
                10L, 5L, ActionType.LIKE, Instant.parse("2026-07-12T12:00:00Z"));
    }

    @Test
    void propagatesCollectorFailureForLike() {
        Event event = occurredEvent(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(event));
        when(requestContract.hasConfirmedParticipation(10L, 5L))
                .thenReturn(new ParticipationRequestExistsResponse(10L, 5L, true));
        StatsClientException failure = new StatsClientException(
                "collectUserAction", Status.UNAVAILABLE.asRuntimeException());
        doThrow(failure).when(collectorClient)
                .collectUserAction(10L, 5L, ActionType.LIKE, Instant.parse("2026-07-12T12:00:00Z"));

        assertThatThrownBy(() -> service.likeEvent(5L, 10L))
                .isSameAs(failure);
    }

    @Test
    void returnsEmptyRecommendationsWithoutDatabaseQuery() {
        when(recommendationsClient.getRecommendationsForUser(10L, 2)).thenReturn(Stream.empty());

        assertThat(service.getRecommendations(10L, null)).isEmpty();

        verify(repository, never()).findAllById(any());
    }

    @Test
    void preservesAnalyzerOrderFiltersEventsAndUsesOneDatabaseQuery() {
        Event first = event(1L);
        Event second = event(2L);
        second.setState(EventState.PENDING);
        Event third = event(3L);
        when(recommendationsClient.getRecommendationsForUser(10L, 5)).thenReturn(Stream.of(
                new RecommendedEvent(3L, 0.9), new RecommendedEvent(4L, 0.8),
                new RecommendedEvent(2L, 0.7), new RecommendedEvent(1L, 0.6)));
        when(repository.findAllById(any())).thenReturn(List.of(first, second, third));

        List<EventShortDto> result = service.getRecommendations(10L, 5);

        assertThat(result).extracting(EventShortDto::getId).containsExactly(3L, 1L);
        assertThat(result).extracting(EventShortDto::getRating).containsExactly(0.9, 0.6);
        verify(repository).findAllById(List.of(3L, 4L, 2L, 1L));
        verify(enricher).enrich(List.of(third, first));
        verifyNoInteractions(ratingService);
    }

    @Test
    void limitsAnalyzerStreamAndRejectsLimitAboveConfiguredMaximum() {
        when(recommendationsClient.getRecommendationsForUser(10L, 2)).thenReturn(Stream.of(
                new RecommendedEvent(1L, 1.0), new RecommendedEvent(2L, 0.9),
                new RecommendedEvent(3L, 0.8)));
        when(repository.findAllById(any())).thenReturn(List.of(event(1L), event(2L)));

        assertThat(service.getRecommendations(10L, null)).hasSize(2);
        assertThatThrownBy(() -> service.getRecommendations(10L, 6))
                .isInstanceOf(BadRequestException.class);
        verify(recommendationsClient).getRecommendationsForUser(10L, 2);
    }

    @Test
    void propagatesAnalyzerFailure() {
        StatsClientException failure = new StatsClientException(
                "getRecommendationsForUser", Status.UNAVAILABLE.asRuntimeException());
        when(recommendationsClient.getRecommendationsForUser(10L, 2)).thenThrow(failure);

        assertThatThrownBy(() -> service.getRecommendations(10L, null))
                .isSameAs(failure);

        verify(repository, never()).findAllById(any());
    }

    @Test
    void performsPublicSearch() {
        Event first = event(1L);
        Event second = event(2L);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second)));
        when(ratingService.getRatings(anyList())).thenReturn(Map.of(1L, 0.8));

        List<EventShortDto> result = service.getPublicEvents(
                null, null, null, null, null, false, null, 0, 10);

        assertThat(result).extracting(EventShortDto::getRating).containsExactly(0.8, 0.0);
        verify(ratingService).getRatings(List.of(first, second));
        verify(collectorClient, never()).collectUserAction(anyLong(), anyLong(), any(), any());
    }

    @Test
    void sendsOneViewWithHeaderUserAndClockTimestamp() {
        Event event = event(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(event));
        when(ratingService.getRating(event)).thenReturn(0.4);

        service.getPublicEventById(7L, 15L);

        verify(collectorClient).collectUserAction(
                15L, 7L, ActionType.VIEW, Instant.parse("2026-07-12T12:00:00Z"));
    }

    @Test
    void doesNotSendViewWhenEventIsMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicEventById(99L, 15L));

        verify(collectorClient, never()).collectUserAction(anyLong(), anyLong(), any(), any());
    }

    @Test
    void doesNotSendViewWhenEventIsNotPublished() {
        Event event = event(8L);
        event.setState(EventState.PENDING);
        when(repository.findById(8L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.getPublicEventById(8L, 15L));

        verify(collectorClient, never()).collectUserAction(anyLong(), anyLong(), any(), any());
    }

    private Event event(Long id) {
        return Event.builder().id(id).title("Event").annotation("Annotation").description("Description")
                .initiatorId(1L).initiatorName("User").categoryId(2L).categoryName("Category")
                .createdOn(LocalDateTime.now()).eventDate(LocalDateTime.now().plusDays(1)).state(EventState.PUBLISHED).build();
    }

    private Event occurredEvent(Long id) {
        Event event = event(id);
        event.setEventDate(LocalDateTime.of(2026, 7, 12, 11, 0));
        return event;
    }
}
