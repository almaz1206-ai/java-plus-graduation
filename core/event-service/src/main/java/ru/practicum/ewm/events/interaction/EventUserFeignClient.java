package ru.practicum.ewm.events.interaction;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.common.*;
import ru.practicum.interaction.user.*;

@FeignClient(name = "user-service", contextId = "eventUserClient", path = "/internal/users")
public interface EventUserFeignClient extends UserContract {
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
