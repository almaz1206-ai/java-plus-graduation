package ru.practicum.ewm.compilation.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.commonview.CategoryView;
import ru.practicum.ewm.commonview.EventShortView;
import ru.practicum.ewm.commonview.UserShortView;
import ru.practicum.interaction.event.EventSummaryResponse;

import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
public class CompilationMapper {

    public CompilationDto toCompilationDto(Compilation compilation, Map<Long, EventSummaryResponse> events) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .events(compilation.getEventIds().stream()
                        .map(events::get).filter(java.util.Objects::nonNull).map(CompilationMapper::toView)
                        .collect(Collectors.toList()))
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }

    public static EventShortView toView(EventSummaryResponse event) {
        return new EventShortView(event.id(), event.title(), event.annotation(),
                new CategoryView(event.category().id(), event.category().name()), event.paid(), event.eventDate(),
                event.confirmedRequests(), event.views(), new UserShortView(event.initiatorId(), event.initiatorName()));
    }

    public static Compilation toCompilation(CompilationDto compilationDto, java.util.Set<Long> events) {
        Compilation compilation = new Compilation();
        compilation.setId(compilation.getId());
        compilation.setEventIds(events);
        compilation.setPinned(compilationDto.getPinned() != null ? compilationDto.getPinned() : false);
        compilation.setTitle(compilationDto.getTitle());

        return compilation;
    }
}
