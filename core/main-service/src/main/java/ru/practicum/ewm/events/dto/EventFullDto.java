package ru.practicum.ewm.events.dto;

import lombok.*;
import ru.practicum.ewm.categories.dto.CategoryDto;
import ru.practicum.ewm.events.model.EventState;
import ru.practicum.ewm.user.dto.UserShortDto;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventFullDto {
    private Long id;
    private String title;
    private String annotation;
    private String description;
    private CategoryDto category;
    private Boolean paid;
    private Integer participantLimit;
    private Boolean requestModeration;
    private LocalDateTime createdOn;
    private LocalDateTime eventDate;
    private LocalDateTime publishedOn;
    private EventState state;
    private Integer confirmedRequests;
    private Long views;
    private UserShortDto initiator;
    private Location location;
}
