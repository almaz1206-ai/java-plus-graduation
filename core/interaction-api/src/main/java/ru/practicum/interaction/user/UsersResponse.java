package ru.practicum.interaction.user;

import java.util.List;

public record UsersResponse(List<UserResponse> users) {
    public UsersResponse {
        users = users == null ? List.of() : List.copyOf(users);
    }
}
