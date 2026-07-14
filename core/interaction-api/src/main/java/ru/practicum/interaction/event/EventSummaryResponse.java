package ru.practicum.interaction.event;

import java.time.LocalDateTime;

public record EventSummaryResponse(Long id, String title, String annotation,
                                   Long initiatorId, String initiatorName,
                                   CategoryResponse category, Boolean paid,
                                   LocalDateTime eventDate, Integer confirmedRequests, Double rating) {
}
