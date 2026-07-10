package ru.practicum.interaction.event;

import ru.practicum.interaction.common.IdsRequest;

public interface CategoryContract {
    CategoryResponse getById(Long categoryId);
    CategoriesResponse getByIds(IdsRequest request);
}
