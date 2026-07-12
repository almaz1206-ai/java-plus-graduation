package ru.practicum.interaction.additional;

import java.util.Set;

public record CompilationEventIdsResponse(Long compilationId, Set<Long> eventIds) {
    public CompilationEventIdsResponse {
        eventIds = eventIds == null ? Set.of() : Set.copyOf(eventIds);
    }
}
