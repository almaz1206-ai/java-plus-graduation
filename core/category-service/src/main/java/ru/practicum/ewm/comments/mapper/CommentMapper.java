package ru.practicum.ewm.comments.mapper;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import ru.practicum.ewm.comments.dto.CommentDto;
import ru.practicum.ewm.comments.dto.NewCommentDto;
import ru.practicum.ewm.comments.model.Comment;
import ru.practicum.ewm.commonview.EventShortView;
import ru.practicum.ewm.commonview.UserShortView;

import java.time.LocalDateTime;

@UtilityClass
public class CommentMapper {
    public @NonNull Comment toComment(NewCommentDto newCommentDto, Long authorId, Long eventId) {
        return Comment.builder()
                .authorId(authorId)
                .eventId(eventId)
                .text(newCommentDto.getText())
                .created(LocalDateTime.now())
                .build();
    }

    public @NonNull CommentDto toCommentDto(Comment comment, UserShortView author, EventShortView event, long likesCount) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(author)
                .event(event)
                .created(comment.getCreated())
                .edited(comment.getEdited())
                .likesCount(likesCount)
                .build();
    }
}
