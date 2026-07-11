package ru.practicum.ewm.events.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.ewm.error.BadRequestException;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.ewm.events.dto.NewEventDto;
import ru.practicum.ewm.events.dto.UpdateEventUserRequest;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.events.model.EventState;
import ru.practicum.ewm.events.repository.EventRepository;
import ru.practicum.interaction.event.CategoryContract;
import ru.practicum.interaction.event.CategoryResponse;
import ru.practicum.interaction.user.UserContract;
import ru.practicum.interaction.user.UserResponse;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventPrivateServiceImplTest {
    @Mock
    EventRepository repository;
    @Mock
    UserContract users;
    @Mock
    CategoryContract categories;
    @Mock
    EventEnricher enricher;
    @InjectMocks
    EventPrivateServiceImpl service;

    @BeforeEach
    void setBusinessParameters() {
        ReflectionTestUtils.setField(service, "userMinHours", 2L);
    }

    @Test
    void createsEvent() {
        when(users.getById(1L)).thenReturn(new UserResponse(1L, "User", "u@example.com"));
        when(categories.getById(2L)).thenReturn(new CategoryResponse(2L, "Category"));
        when(repository.save(any(Event.class))).thenAnswer(i -> {
            Event e = i.getArgument(0);
            e.setId(3L);
            return e;
        });
        assertThat(service.addEvent(1L, newEvent()).getId()).isEqualTo(3L);
    }

    @Test
    void updatesEvent() {
        Event event = event(EventState.PENDING);
        when(repository.findByIdAndInitiatorId(3L, 1L)).thenReturn(Optional.of(event));
        when(repository.save(event)).thenReturn(event);
        assertThat(service.updateUserEvent(1L, 3L, UpdateEventUserRequest.builder().title("Updated").build()).getTitle()).isEqualTo("Updated");
    }

    @Test
    void rejectsInvalidDate() {
        NewEventDto dto = newEvent();
        dto.setEventDate(LocalDateTime.now().plusMinutes(10));
        assertThatThrownBy(() -> service.addEvent(1L, dto)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void reportsMissingUser() {
        when(users.getById(1L)).thenThrow(new NotFoundException("missing"));
        assertThatThrownBy(() -> service.addEvent(1L, newEvent())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void reportsMissingCategory() {
        when(users.getById(1L)).thenReturn(new UserResponse(1L, "User", "u@example.com"));
        when(categories.getById(2L)).thenThrow(new NotFoundException("missing"));
        assertThatThrownBy(() -> service.addEvent(1L, newEvent())).isInstanceOf(NotFoundException.class);
    }

    private NewEventDto newEvent() {
        NewEventDto d = new NewEventDto();
        d.setTitle("Event");
        d.setAnnotation("A".repeat(20));
        d.setDescription("D".repeat(20));
        d.setCategoryId(2L);
        d.setEventDate(LocalDateTime.now().plusDays(1));
        return d;
    }

    private Event event(EventState state) {
        return Event.builder().id(3L).title("Event").annotation("A".repeat(20)).description("D".repeat(20))
                .initiatorId(1L).initiatorName("User").categoryId(2L).categoryName("Category")
                .createdOn(LocalDateTime.now()).eventDate(LocalDateTime.now().plusDays(1)).state(state).build();
    }
}
