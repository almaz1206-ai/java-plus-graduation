package ru.practicum.ewm.request.dto;

import lombok.*;
import ru.practicum.ewm.request.model.StatusRequest;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDto {
    private Long id;
    private Long requester;
    private Long event;
    private StatusRequest status;
    private LocalDateTime created;
}


