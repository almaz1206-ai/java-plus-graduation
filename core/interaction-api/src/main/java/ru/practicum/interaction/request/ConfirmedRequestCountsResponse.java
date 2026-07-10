package ru.practicum.interaction.request;

import java.util.List;

public record ConfirmedRequestCountsResponse(List<ConfirmedRequestCountResponse> counts) {
    public ConfirmedRequestCountsResponse {
        counts = counts == null ? List.of() : List.copyOf(counts);
    }
}
