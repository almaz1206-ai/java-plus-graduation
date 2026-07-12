package ru.practicum.interaction.request;

import ru.practicum.interaction.common.IdsRequest;

public interface RequestContract {
    ConfirmedRequestCountResponse getConfirmedCount(Long eventId);

    ConfirmedRequestCountsResponse getConfirmedCounts(IdsRequest eventIds);

    ParticipationRequestExistsResponse exists(Long userId, Long eventId);

    RequestStatusesResponse getStatuses(IdsRequest requestIds);
}
