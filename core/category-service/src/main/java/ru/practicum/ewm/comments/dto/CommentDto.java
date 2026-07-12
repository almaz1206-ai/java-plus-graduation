package ru.practicum.ewm.comments.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.commonview.EventShortView;
import ru.practicum.ewm.commonview.UserShortView;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentDto {
    private Long id;
    private String text;
    private UserShortView author;
    private EventShortView event;
    private LocalDateTime created;
    private LocalDateTime edited;
    private Long likesCount;
}
