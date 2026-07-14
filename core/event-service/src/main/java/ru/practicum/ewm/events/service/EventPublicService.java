package ru.practicum.ewm.events.service;

import ru.practicum.ewm.events.dto.EventFullDto;
import ru.practicum.ewm.events.dto.EventShortDto;
import ru.practicum.ewm.events.model.EventSort;

import java.time.LocalDateTime;
import java.util.List;

public interface EventPublicService {
    void likeEvent(Long eventId, long userId);

    List<EventShortDto> getRecommendations(long userId, Integer size);

    List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable,
                                        EventSort sort, int from, int size);

    EventFullDto getPublicEventById(Long eventId, long userId);
}
