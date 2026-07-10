package ru.practicum.interaction.common;

import java.util.Set;

public record IdsRequest(Set<Long> ids) {
    public IdsRequest {
        ids = ids == null ? Set.of() : Set.copyOf(ids);
    }
}
