package ru.practicum.ewm.events.interaction;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.event.*;

@FeignClient(name = "category-service", contextId = "eventCategoryClient", path = "/internal/categories")
public interface EventCategoryFeignClient extends CategoryContract {
    @Override
    @GetMapping("/{categoryId}")
    CategoryResponse getById(@PathVariable("categoryId") Long categoryId);

    @Override
    @PostMapping("/by-ids")
    CategoriesResponse getByIds(@RequestBody IdsRequest request);
}
