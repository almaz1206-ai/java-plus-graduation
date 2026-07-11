package ru.practicum.ewm.compilation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.event.CategoryResponse;
import ru.practicum.interaction.event.EventContract;
import ru.practicum.interaction.event.EventSummaryResponse;
import ru.practicum.interaction.event.EventsResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {
    @Mock
    CompilationRepository repository;
    @Mock
    EventContract eventContract;

    @Test
    void createsCompilationUsingBatchEventLookup() {
        EventSummaryResponse first = event(1L);
        EventSummaryResponse second = event(2L);
        when(eventContract.getByIds(any(IdsRequest.class))).thenReturn(new EventsResponse(List.of(first, second)));
        when(repository.save(any(Compilation.class))).thenAnswer(invocation -> {
            Compilation value = invocation.getArgument(0);
            value.setId(10L);
            return value;
        });
        NewCompilationDto request = new NewCompilationDto(Set.of(1L, 2L), true, "Compilation");
        var result = new CompilationServiceImpl(repository, eventContract).saveCompilation(request);
        assertThat(result.getEvents()).hasSize(2);
        verify(eventContract, times(1)).getByIds(any(IdsRequest.class));
    }

    private EventSummaryResponse event(Long id) {
        return new EventSummaryResponse(id, "Event " + id, "Annotation", 1L, "User",
                new CategoryResponse(1L, "Category"), false, LocalDateTime.now(), 0, 0L);
    }
}
