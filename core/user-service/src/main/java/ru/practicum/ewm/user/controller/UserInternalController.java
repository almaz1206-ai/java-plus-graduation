package ru.practicum.ewm.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.interaction.common.ExistenceResponse;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.user.UserContract;
import ru.practicum.interaction.user.UserResponse;
import ru.practicum.interaction.user.UsersResponse;

import java.util.Set;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserInternalController {
    private final UserContract contract;

    @GetMapping("/{userId}")
    public UserResponse getById(@PathVariable Long userId) {
        return contract.getById(userId);
    }

    @GetMapping
    public UsersResponse getByIds(@RequestParam Set<Long> ids) {
        return contract.getByIds(new IdsRequest(ids));
    }

    @PostMapping("/by-ids")
    public UsersResponse getByIds(@RequestBody IdsRequest request) {
        return contract.getByIds(request);
    }

    @GetMapping("/{userId}/exists")
    public ExistenceResponse exists(@PathVariable Long userId) {
        return contract.exists(userId);
    }
}
