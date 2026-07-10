package ru.practicum.ewm.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.request.*;

@RestController @RequestMapping("/internal/requests") @RequiredArgsConstructor
public class RequestInternalController {
    private final RequestContract contract;
    @GetMapping("/events/{eventId}/confirmed-count") public ConfirmedRequestCountResponse count(@PathVariable Long eventId) { return contract.getConfirmedCount(eventId); }
    @PostMapping("/events/confirmed-counts") public ConfirmedRequestCountsResponse counts(@RequestBody IdsRequest ids) { return contract.getConfirmedCounts(ids); }
    @GetMapping("/users/{userId}/events/{eventId}/exists") public ParticipationRequestExistsResponse exists(@PathVariable Long userId, @PathVariable Long eventId) { return contract.exists(userId, eventId); }
    @PostMapping("/events/statuses") public RequestStatusesResponse statuses(@RequestBody IdsRequest ids) { return contract.getStatuses(ids); }
}
