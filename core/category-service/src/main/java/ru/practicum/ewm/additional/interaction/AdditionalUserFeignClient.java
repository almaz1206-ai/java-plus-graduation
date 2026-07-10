package ru.practicum.ewm.additional.interaction;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.interaction.common.ExistenceResponse;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.user.UserContract;
import ru.practicum.interaction.user.UserResponse;
import ru.practicum.interaction.user.UsersResponse;

@FeignClient(name = "user-service", contextId = "additionalUserClient", path = "/internal/users")
public interface AdditionalUserFeignClient extends UserContract {
    @Override
    @GetMapping("/{userId}")
    UserResponse getById(@PathVariable("userId") Long userId);

    @Override
    @PostMapping("/by-ids")
    UsersResponse getByIds(@RequestBody IdsRequest request);

    @Override
    @GetMapping("/{userId}/exists")
    ExistenceResponse exists(@PathVariable("userId") Long userId);
}
