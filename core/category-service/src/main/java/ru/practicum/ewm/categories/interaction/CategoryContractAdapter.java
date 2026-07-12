package ru.practicum.ewm.categories.interaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.categories.model.Category;
import ru.practicum.ewm.categories.repository.CategoryRepository;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.event.*;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryContractAdapter implements CategoryContract {
    private final CategoryRepository repository;

    @Override
    public CategoryResponse getById(Long id) {
        return repository.findById(id).map(this::map)
                .orElseThrow(() -> new NotFoundException("Category with id=" + id + " was not found"));
    }

    @Override
    public CategoriesResponse getByIds(IdsRequest request) {
        return new CategoriesResponse(repository.findAllById(request.ids()).stream().map(this::map).toList());
    }

    private CategoryResponse map(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
