package ru.practicum.ewm.categories.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.event.*;

@RestController
@RequestMapping("/internal/categories")
@RequiredArgsConstructor
public class CategoryInternalController {
    private final CategoryContract contract;

    @GetMapping("/{categoryId}")
    public CategoryResponse get(@PathVariable Long categoryId) {
        return contract.getById(categoryId);
    }

    @PostMapping("/by-ids")
    public CategoriesResponse getByIds(@RequestBody IdsRequest ids) {
        return contract.getByIds(ids);
    }
}
