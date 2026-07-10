package ru.practicum.interaction.event;

import java.util.List;

public record CategoriesResponse(List<CategoryResponse> categories) {
    public CategoriesResponse {
        categories = categories == null ? List.of() : List.copyOf(categories);
    }
}
