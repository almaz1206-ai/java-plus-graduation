package ru.practicum.ewm.user.interaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;
import ru.practicum.interaction.common.ExistenceResponse;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.user.UserContract;
import ru.practicum.interaction.user.UserResponse;
import ru.practicum.interaction.user.UsersResponse;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserContractAdapter implements UserContract {
    private final UserRepository repository;

    @Override
    public UserResponse getById(Long userId) {
        return repository.findById(userId).map(this::map)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    @Override
    public UsersResponse getByIds(IdsRequest request) {
        return new UsersResponse(repository.findAllById(request.ids()).stream().map(this::map).toList());
    }

    @Override
    public ExistenceResponse exists(Long userId) {
        return new ExistenceResponse(userId, repository.existsById(userId));
    }

    private UserResponse map(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
