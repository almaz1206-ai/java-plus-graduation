package ru.practicum.interaction.event;

import ru.practicum.interaction.common.IdsRequest;

public interface EventContract {
    EventSummaryResponse getById(Long eventId);

    EventsResponse getByIds(IdsRequest request);

    EventParticipationResponse getParticipationDetails(Long eventId);

    boolean isInitiator(Long eventId, Long userId);

    boolean hasState(Long eventId, EventState state);

    int getParticipantLimit(Long eventId);

    int changeConfirmedRequests(Long eventId, int delta);

    boolean existsByCategoryId(Long categoryId);
}
