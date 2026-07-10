package ru.practicum.interaction.user;

import ru.practicum.interaction.common.ExistenceResponse;
import ru.practicum.interaction.common.IdsRequest;

public interface UserContract {
    UserResponse getById(Long userId);
    UsersResponse getByIds(IdsRequest request);
    ExistenceResponse exists(Long userId);
}
