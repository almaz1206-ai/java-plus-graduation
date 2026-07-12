package ru.practicum.ewm.commonview;

import java.time.LocalDateTime;

public record EventShortView(Long id, String title, String annotation, CategoryView category,
                             Boolean paid, LocalDateTime eventDate, Integer confirmedRequests,
                             Long views, UserShortView initiator) {
}
