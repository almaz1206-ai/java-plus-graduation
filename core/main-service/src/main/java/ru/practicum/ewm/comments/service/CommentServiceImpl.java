package ru.practicum.ewm.comments.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import ru.practicum.ewm.error.ConflictException;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.ewm.events.dto.EventShortDto;
import ru.practicum.ewm.events.mapper.EventMapper;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.events.model.EventState;
import ru.practicum.ewm.events.repository.EventRepository;
import ru.practicum.ewm.user.dto.UserShortDto;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentLikeRepository commentLikeRepository;

    @Override
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.info("Creating comment for user: {}, event: {}", userId, eventId);
        User author = checkAndGetUser(userId);
        Event event = checkAndGetEvent(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Comments are only allowed on published events.");
        }

        Comment comment = commentRepository.save(CommentMapper.toComment(newCommentDto, author, event));
        log.debug("Comment created with id: {}", comment.getId());
        UserShortDto userShort = UserMapper.toUserShortDto(author);
        EventShortDto eventShort = EventMapper.toEventShortDto(event);

        return CommentMapper.toCommentDto(comment, userShort, eventShort, 0L);
    }

    @Override
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto newCommentDto) {
        User author = checkAndGetUser(userId);
        Comment comment = checkAndGetComment(commentId);
        log.info("Updating comment for user: {}, commentId: {}", userId, commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Only the author can edit the comment.");
        }

        comment.setText(newCommentDto.getText());
        comment.setEdited(LocalDateTime.now());
        UserShortDto userShort = UserMapper.toUserShortDto(author);
        EventShortDto eventShort = EventMapper.toEventShortDto(comment.getEvent());
        long countLikes = commentLikeRepository.countByCommentId(comment.getId());

        return CommentMapper.toCommentDto(comment, userShort, eventShort, countLikes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByAuthorId(Long userId, Integer from, Integer size, Sort sort) {
        log.info("Getting comments from user sorted by likes: userId={}, from={}, size={}",
                userId, from, size);

        User author = checkAndGetUser(userId);
        UserShortDto userShort = UserMapper.toUserShortDto(author);

        Pageable pageable = new OffsetPageRequest(from, size);

        Page<Comment> page = switch (sort) {
            case ASC -> commentRepository.findAllByAuthorIdOrderByLikesAsc(userId, pageable);
            case DESC -> commentRepository.findAllByAuthorIdOrderByLikesDesc(userId, pageable);
        };

        List<Comment> comments = page.getContent();

        if (comments.isEmpty()) {
            return List.of();
        }

        List<Long> ids = comments.stream()
                .map(Comment::getId)
                .toList();

        Map<Long, Long> likesMap = commentLikeRepository.countLikesForComments(ids)
                .stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (Long) r[1]
                ));

        return comments.stream()
                .map(c -> CommentMapper.toCommentDto(
                        c,
                        userShort,
                        EventMapper.toEventShortDto(c.getEvent()),
                        likesMap.getOrDefault(c.getId(), 0L)
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByEventId(Long eventId, Integer from, Integer size, Sort sort) {
        log.info("Getting comments for event sorted by likes: eventId={}, from={}, size={}",
                eventId, from, size);

        Event event = checkAndGetEvent(eventId);
        EventShortDto eventShort = EventMapper.toEventShortDto(event);

        Pageable pageable = new OffsetPageRequest(from, size);

        Page<Comment> page = switch (sort) {
            case ASC -> commentRepository.findAllByEventIdOrderByLikesAsc(eventId, pageable);
            case DESC -> commentRepository.findAllByEventIdOrderByLikesDesc(eventId, pageable);
        };

        List<Comment> comments = page.getContent();

        if (comments.isEmpty()) {
            return List.of();
        }

        List<Long> ids = comments.stream()
                .map(Comment::getId)
                .toList();

        Map<Long, Long> likesMap = commentLikeRepository.countLikesForComments(ids)
                .stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (Long) r[1]
                ));

        return comments.stream()
                .map(c -> CommentMapper.toCommentDto(
                        c,
                        UserMapper.toUserShortDto(c.getAuthor()),
                        eventShort,
                        likesMap.getOrDefault(c.getId(), 0L)
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDto getCommentById(Long commentId) {
        log.info("Getting comment with id={}", commentId);
        Comment comment = checkAndGetComment(commentId);
        UserShortDto userShort = UserMapper.toUserShortDto(comment.getAuthor());
        EventShortDto eventShort = EventMapper.toEventShortDto(comment.getEvent());
        long countLikes = commentLikeRepository.countByCommentId(comment.getId());

        return CommentMapper.toCommentDto(comment, userShort, eventShort, countLikes);
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        log.info("Delete comment by a user: userId={}, commentId={}", userId, commentId);
        Comment comment = checkAndGetComment(commentId);
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Only author can delete the comment.");
        }

        commentLikeRepository.deleteAllByCommentId(commentId);
        commentRepository.deleteById(commentId);
    }

    @Override
    public void deleteComment(Long commentId) {
        log.info("Delete comment with id={}", commentId);
        checkAndGetComment(commentId);
        commentLikeRepository.deleteAllByCommentId(commentId);
        commentRepository.deleteById(commentId);
    }

    @Override
    public CommentDto addLike(Long userId, Long commentId) {
        User author = checkAndGetUser(userId);
        Comment comment = checkAndGetComment(commentId);

        if (comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("You cannot like your own comment.");
        }

        if (commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            throw new ConflictException("You already liked this comment");
        }

        CommentLike like = CommentLike.builder()
                .user(author)
                .comment(comment)
                .build();

        commentLikeRepository.save(like);

        long likesCount = commentLikeRepository.countByCommentId(commentId);

        UserShortDto userShort = UserMapper.toUserShortDto(author);
        EventShortDto eventShort = EventMapper.toEventShortDto(comment.getEvent());

        return CommentMapper.toCommentDto(comment, userShort, eventShort, likesCount);
    }

    @Override
    public void deleteLike(Long userId, Long commentId) {
        CommentLike like = commentLikeRepository
                .findByUserIdAndCommentId(userId, commentId)
                .orElseThrow(() -> new NotFoundException("Like not found"));

        commentLikeRepository.delete(like);
    }


    private Comment checkAndGetComment(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(() ->
                new NotFoundException("Comment with id=" + commentId + " was not found"));
    }

    private User checkAndGetUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User with id=" + userId + " was not found"));
    }

    private Event checkAndGetEvent(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() ->
                new NotFoundException("Event with id=" + eventId + " was not found"));
    }
}