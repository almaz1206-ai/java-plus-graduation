package ru.practicum.ewm.comments.service;

import ru.practicum.ewm.comments.dto.CommentDto;
import ru.practicum.ewm.comments.dto.NewCommentDto;
import ru.practicum.ewm.comments.model.Sort;

import java.util.List;

public interface CommentService {
    CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    CommentDto updateComment(Long userId, Long commentId, NewCommentDto newCommentDto);

    List<CommentDto> getCommentsByAuthorId(Long userId, Integer from, Integer size, Sort sort);

    List<CommentDto> getCommentsByEventId(Long eventId, Integer from, Integer size, Sort sort);

    CommentDto getCommentById(Long commentId);

    void deleteComment(Long userId, Long commentId);

    void deleteComment(Long commentId);

    CommentDto addLike(Long userId, Long commentId);

    void deleteLike(Long userId, Long commentId);
}
