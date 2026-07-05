package ru.practicum.ewm.events.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import ru.practicum.ewm.events.model.StateAction;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventUserRequest {

    @Size(min = 3, max = 120)
    private String title;

    @Size(min = 20, max = 2000)
    private String annotation;

    @Size(min = 20, max = 7000)
    private String description;

    @JsonProperty("category")
    private Long categoryId;

    private Location location;

    private Boolean paid;

    private LocalDateTime eventDate;

    @Min(0)
    private Integer participantLimit;

    private Boolean requestModeration;

    private StateAction stateAction;
}