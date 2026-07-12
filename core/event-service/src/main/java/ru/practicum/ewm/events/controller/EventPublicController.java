package ru.practicum.ewm.events.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.events.dto.EventFullDto;
import ru.practicum.ewm.events.dto.EventShortDto;
import ru.practicum.ewm.events.model.EventSort;
import ru.practicum.ewm.events.service.EventPublicService;

import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventPublicController {

    private final EventPublicService eventService;

    @PutMapping("/{eventId}/like")
    public void likeEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") long userId) {
        eventService.likeEvent(eventId, userId);
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") long userId,
            @RequestParam(required = false) @Min(1) Integer size) {
        return eventService.getRecommendations(userId, size);
    }

    @GetMapping
    public List<EventShortDto> getPublicEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) EventSort sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        return eventService.getPublicEvents(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
    }

    @GetMapping("/{id}")
    public EventFullDto getPublicEventById(
            @PathVariable Long id,
            @RequestHeader("X-EWM-USER-ID") long userId) {
        return eventService.getPublicEventById(id, userId);
    }
}
