package ru.practicum.ewm.compilation.dto;

import lombok.*;
import ru.practicum.ewm.commonview.EventShortView;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationDto {
    private Long id;
    private List<EventShortView> events;
    private Boolean pinned;
    private String title;
}
