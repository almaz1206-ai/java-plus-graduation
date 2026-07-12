package ru.practicum.interaction.request;

import java.util.List;

public record RequestStatusesResponse(List<RequestStatusResponse> requests) {
    public RequestStatusesResponse {
        requests = requests == null ? List.of() : List.copyOf(requests);
    }
}
