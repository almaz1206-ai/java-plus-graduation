package ru.practicum.interaction.event;

import java.util.List;

public record EventsResponse(List<EventSummaryResponse> events) {
    public EventsResponse {
        events = events == null ? List.of() : List.copyOf(events);
    }
}
