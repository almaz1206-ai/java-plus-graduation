package ru.practicum.ewm.compilation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.interaction.additional.CompilationEventIdsResponse;

@RestController
@RequestMapping("/internal/compilations")
@RequiredArgsConstructor
public class CompilationInternalController {
    private final CompilationRepository repository;

    @GetMapping("/{compilationId}/event-ids")
    public CompilationEventIdsResponse eventIds(@PathVariable Long compilationId) {
        Compilation compilation = repository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compilationId + " was not found"));
        return new CompilationEventIdsResponse(compilationId, compilation.getEventIds());
    }
}
