package ru.practicum.ewm.events.interaction;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.request.*;

@FeignClient(name = "request-service", contextId = "eventRequestClient", path = "/internal/requests")
public interface EventRequestFeignClient extends RequestContract {
    @Override
    @GetMapping("/events/{eventId}/confirmed-count")
    ConfirmedRequestCountResponse getConfirmedCount(@PathVariable("eventId") Long eventId);

    @Override
    @PostMapping("/events/confirmed-counts")
    ConfirmedRequestCountsResponse getConfirmedCounts(@RequestBody IdsRequest ids);

    @Override
    @GetMapping("/users/{userId}/events/{eventId}/exists")
    ParticipationRequestExistsResponse exists(@PathVariable("userId") Long userId, @PathVariable("eventId") Long eventId);

    @Override
    @GetMapping("/users/{userId}/events/{eventId}/confirmed")
    ParticipationRequestExistsResponse hasConfirmedParticipation(
            @PathVariable("userId") Long userId,
            @PathVariable("eventId") Long eventId);

    @Override
    @PostMapping("/events/statuses")
    RequestStatusesResponse getStatuses(@RequestBody IdsRequest ids);
}
