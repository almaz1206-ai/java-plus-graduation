package ru.practicum.ewm.additional.interaction;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.event.*;

@FeignClient(name = "event-service", contextId = "additionalEventClient", path = "/internal/events")
public interface AdditionalEventFeignClient extends EventContract {
    @Override
    @GetMapping("/{eventId}")
    EventSummaryResponse getById(@PathVariable("eventId") Long eventId);

    @Override
    @PostMapping("/by-ids")
    EventsResponse getByIds(@RequestBody IdsRequest request);

    @Override
    @GetMapping("/{eventId}/participation")
    EventParticipationResponse getParticipationDetails(@PathVariable("eventId") Long eventId);

    @Override
    @GetMapping("/{eventId}/initiator/{userId}")
    boolean isInitiator(@PathVariable("eventId") Long eventId, @PathVariable("userId") Long userId);

    @Override
    @GetMapping("/{eventId}/state/{state}")
    boolean hasState(@PathVariable("eventId") Long eventId, @PathVariable("state") EventState state);

    @Override
    @GetMapping("/{eventId}/participant-limit")
    int getParticipantLimit(@PathVariable("eventId") Long eventId);

    @Override
    @PostMapping("/{eventId}/confirmed-requests")
    int changeConfirmedRequests(@PathVariable("eventId") Long eventId, @RequestParam("delta") int delta);

    @Override
    @GetMapping("/category/{categoryId}/exists")
    boolean existsByCategoryId(@PathVariable("categoryId") Long categoryId);
}
