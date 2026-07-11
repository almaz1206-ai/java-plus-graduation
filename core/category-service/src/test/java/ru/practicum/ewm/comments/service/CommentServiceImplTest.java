package ru.practicum.ewm.comments.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.comments.dto.NewCommentDto;
import ru.practicum.ewm.comments.model.Comment;
import ru.practicum.ewm.comments.repository.CommentLikeRepository;
import ru.practicum.ewm.comments.repository.CommentRepository;
import ru.practicum.interaction.event.*;
import ru.practicum.interaction.user.UserContract;
import ru.practicum.interaction.user.UserResponse;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {
    @Mock
    CommentRepository comments;
    @Mock
    CommentLikeRepository likes;
    @Mock
    UserContract users;
    @Mock
    EventContract events;

    @Test
    void createsCommentForPublishedEvent() {
        when(users.getById(1L)).thenReturn(new UserResponse(1L, "User", "user@example.com"));
        when(events.hasState(2L, EventState.PUBLISHED)).thenReturn(true);
        when(events.getById(2L)).thenReturn(event());
        when(comments.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment value = invocation.getArgument(0);
            value.setId(3L);
            return value;
        });
        var service = new CommentServiceImpl(comments, likes, users, events);
        var result = service.createComment(1L, 2L, new NewCommentDto("Comment"));
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getAuthor().id()).isEqualTo(1L);
    }

    private EventSummaryResponse event() {
        return new EventSummaryResponse(2L, "Event", "Annotation", 4L, "Initiator",
                new CategoryResponse(5L, "Category"), false, LocalDateTime.now(), 0, 0L);
    }
}
