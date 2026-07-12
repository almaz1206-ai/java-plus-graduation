package ru.practicum.ewm.comments.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comments.dto.CommentDto;
import ru.practicum.ewm.comments.dto.NewCommentDto;
import ru.practicum.ewm.comments.mapper.CommentMapper;
import ru.practicum.ewm.comments.model.Comment;
import ru.practicum.ewm.comments.model.CommentLike;
import ru.practicum.ewm.comments.model.Sort;
import ru.practicum.ewm.comments.repository.CommentLikeRepository;
import ru.practicum.ewm.comments.repository.CommentRepository;
import ru.practicum.ewm.common.OffsetPageRequest;
import ru.practicum.ewm.commonview.EventShortView;
import ru.practicum.ewm.commonview.UserShortView;
import ru.practicum.ewm.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.error.ConflictException;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.event.EventContract;
import ru.practicum.interaction.event.EventState;
import ru.practicum.interaction.event.EventSummaryResponse;
import ru.practicum.interaction.user.UserContract;
import ru.practicum.interaction.user.UserResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final CommentLikeRepository likeRepository;
    private final UserContract userContract;
    private final EventContract eventContract;

    @Override
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        UserResponse user = userContract.getById(userId);
        if (!eventContract.hasState(eventId, EventState.PUBLISHED)) {
            throw new ConflictException("Comments are only allowed on published events.");
        }
        EventSummaryResponse event = eventContract.getById(eventId);
        Comment comment = commentRepository.save(CommentMapper.toComment(dto, userId, eventId));
        return map(comment, user, event, 0L);
    }

    @Override
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto dto) {
        UserResponse user = userContract.getById(userId);
        Comment comment = comment(commentId);
        requireAuthor(comment, userId);
        comment.setText(dto.getText());
        comment.setEdited(LocalDateTime.now());
        return map(comment, user, eventContract.getById(comment.getEventId()), likeRepository.countByCommentId(commentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByAuthorId(Long userId, Integer from, Integer size, Sort sort) {
        UserResponse author = userContract.getById(userId);
        List<Comment> comments = authorPage(userId, new OffsetPageRequest(from, size), sort).getContent();
        if (comments.isEmpty()) return List.of();
        Map<Long, EventSummaryResponse> events = events(comments);
        Map<Long, Long> likes = likes(comments);
        return comments.stream().map(c -> map(c, author, events.get(c.getEventId()), likes.getOrDefault(c.getId(), 0L))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByEventId(Long eventId, Integer from, Integer size, Sort sort) {
        EventSummaryResponse event = eventContract.getById(eventId);
        List<Comment> comments = eventPage(eventId, new OffsetPageRequest(from, size), sort).getContent();
        if (comments.isEmpty()) return List.of();
        Map<Long, UserResponse> users = users(comments);
        Map<Long, Long> likes = likes(comments);
        return comments.stream().map(c -> map(c, users.get(c.getAuthorId()), event, likes.getOrDefault(c.getId(), 0L))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDto getCommentById(Long commentId) {
        Comment comment = comment(commentId);
        return map(comment, userContract.getById(comment.getAuthorId()), eventContract.getById(comment.getEventId()),
                likeRepository.countByCommentId(commentId));
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = comment(commentId);
        requireAuthor(comment, userId);
        delete(commentId);
    }

    @Override
    public void deleteComment(Long commentId) {
        comment(commentId);
        delete(commentId);
    }

    @Override
    public CommentDto addLike(Long userId, Long commentId) {
        UserResponse user = userContract.getById(userId);
        Comment comment = comment(commentId);
        if (comment.getAuthorId().equals(userId)) throw new ConflictException("You cannot like your own comment.");
        if (likeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            throw new ConflictException("You already liked this comment");
        }
        likeRepository.save(CommentLike.builder().userId(userId).comment(comment).build());
        return map(comment, userContract.getById(comment.getAuthorId()), eventContract.getById(comment.getEventId()),
                likeRepository.countByCommentId(commentId));
    }

    @Override
    public void deleteLike(Long userId, Long commentId) {
        CommentLike like = likeRepository.findByUserIdAndCommentId(userId, commentId)
                .orElseThrow(() -> new NotFoundException("Like not found"));
        likeRepository.delete(like);
    }

    private Page<Comment> authorPage(Long id, Pageable page, Sort sort) {
        return sort == Sort.ASC ? commentRepository.findAllByAuthorIdOrderByLikesAsc(id, page)
                : commentRepository.findAllByAuthorIdOrderByLikesDesc(id, page);
    }

    private Page<Comment> eventPage(Long id, Pageable page, Sort sort) {
        return sort == Sort.ASC ? commentRepository.findAllByEventIdOrderByLikesAsc(id, page)
                : commentRepository.findAllByEventIdOrderByLikesDesc(id, page);
    }

    private Map<Long, UserResponse> users(List<Comment> comments) {
        return userContract.getByIds(new IdsRequest(comments.stream().map(Comment::getAuthorId).collect(Collectors.toSet())))
                .users().stream().collect(Collectors.toMap(UserResponse::id, Function.identity()));
    }

    private Map<Long, EventSummaryResponse> events(List<Comment> comments) {
        return eventContract.getByIds(new IdsRequest(comments.stream().map(Comment::getEventId).collect(Collectors.toSet())))
                .events().stream().collect(Collectors.toMap(EventSummaryResponse::id, Function.identity()));
    }

    private Map<Long, Long> likes(List<Comment> comments) {
        return likeRepository.countLikesForComments(comments.stream().map(Comment::getId).toList()).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    private CommentDto map(Comment comment, UserResponse user, EventSummaryResponse event, long likes) {
        UserShortView author = new UserShortView(user.id(), user.name());
        EventShortView eventView = CompilationMapper.toView(event);
        return CommentMapper.toCommentDto(comment, author, eventView, likes);
    }

    private Comment comment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + id + " was not found"));
    }

    private void requireAuthor(Comment comment, Long userId) {
        if (!comment.getAuthorId().equals(userId))
            throw new ConflictException("Only the author can modify the comment.");
    }

    private void delete(Long id) {
        likeRepository.deleteAllByCommentId(id);
        commentRepository.deleteById(id);
    }
}
