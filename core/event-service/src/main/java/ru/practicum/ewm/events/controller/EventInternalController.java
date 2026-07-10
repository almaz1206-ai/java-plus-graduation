package ru.practicum.ewm.events.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.event.*;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class EventInternalController {
    private final EventContract contract;

    @GetMapping("/{eventId}")
    public EventSummaryResponse get(@PathVariable Long eventId) {
        return contract.getById(eventId);
    }

    @PostMapping("/by-ids")
    public EventsResponse getByIds(@RequestBody IdsRequest ids) {
        return contract.getByIds(ids);
    }

    @GetMapping("/{eventId}/participation")
    public EventParticipationResponse participation(@PathVariable Long eventId) {
        return contract.getParticipationDetails(eventId);
    }

    @GetMapping("/{eventId}/participation-info")
    public EventParticipationResponse participationInfo(@PathVariable Long eventId) {
        return contract.getParticipationDetails(eventId);
    }

    @GetMapping("/{eventId}/owner")
    public EventOwnerResponse owner(@PathVariable Long eventId) {
        EventParticipationResponse event = contract.getParticipationDetails(eventId);
        return new EventOwnerResponse(eventId, event.initiatorId());
    }

    @GetMapping("/{eventId}/initiator/{userId}")
    public boolean initiator(@PathVariable Long eventId, @PathVariable Long userId) {
        return contract.isInitiator(eventId, userId);
    }

    @GetMapping("/{eventId}/state/{state}")
    public boolean state(@PathVariable Long eventId, @PathVariable EventState state) {
        return contract.hasState(eventId, state);
    }

    @GetMapping("/{eventId}/participant-limit")
    public int limit(@PathVariable Long eventId) {
        return contract.getParticipantLimit(eventId);
    }

    @PostMapping("/{eventId}/confirmed-requests")
    public int change(@PathVariable Long eventId, @RequestParam int delta) {
        return contract.changeConfirmedRequests(eventId, delta);
    }

    @GetMapping("/category/{categoryId}/exists")
    public boolean categoryInUse(@PathVariable Long categoryId) {
        return contract.existsByCategoryId(categoryId);
    }
}
