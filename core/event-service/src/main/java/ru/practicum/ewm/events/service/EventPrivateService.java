package ru.practicum.ewm.events.service;

import ru.practicum.ewm.events.dto.*;

import java.util.List;

public interface EventPrivateService {
    List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size);

    EventFullDto addEvent(Long userId, NewEventDto newEventDto);

    EventFullDto getUserEventById(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request);

}
