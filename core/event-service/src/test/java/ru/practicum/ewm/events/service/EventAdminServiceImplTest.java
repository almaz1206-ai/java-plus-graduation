package ru.practicum.ewm.events.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.ewm.error.ConflictException;
import ru.practicum.ewm.events.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.events.model.*;
import ru.practicum.ewm.events.repository.EventRepository;
import ru.practicum.interaction.event.CategoryContract;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventAdminServiceImplTest {
    @Mock EventRepository repository; @Mock CategoryContract categories; @Mock EventEnricher enricher;
    @InjectMocks EventAdminServiceImpl service;

    @BeforeEach
    void setBusinessParameters() {
        ReflectionTestUtils.setField(service, "adminMinHours", 1L);
    }

    @Test void publishesPendingEvent() {
        Event event=event(EventState.PENDING); when(repository.findById(1L)).thenReturn(Optional.of(event)); when(repository.save(event)).thenReturn(event);
        UpdateEventAdminRequest request=new UpdateEventAdminRequest(); request.setStateAction(StateAction.PUBLISH_EVENT);
        assertThat(service.updateEventAdmin(1L, request).getState()).isEqualTo(EventState.PUBLISHED);
    }
    @Test void rejectsPendingEvent() {
        Event event=event(EventState.PENDING); when(repository.findById(1L)).thenReturn(Optional.of(event)); when(repository.save(event)).thenReturn(event);
        UpdateEventAdminRequest request=new UpdateEventAdminRequest(); request.setStateAction(StateAction.REJECT_EVENT);
        assertThat(service.updateEventAdmin(1L, request).getState()).isEqualTo(EventState.CANCELED);
    }
    @Test void rejectsInvalidState() {
        Event event=event(EventState.PUBLISHED); when(repository.findById(1L)).thenReturn(Optional.of(event));
        UpdateEventAdminRequest request=new UpdateEventAdminRequest(); request.setStateAction(StateAction.PUBLISH_EVENT);
        assertThatThrownBy(() -> service.updateEventAdmin(1L, request)).isInstanceOf(ConflictException.class);
    }
    @Test void performsAdministrativeSearch() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(event(EventState.PENDING))));
        assertThat(service.getEventsAdmin(null, null, null, null, null, 0, 10)).hasSize(1);
    }
    private Event event(EventState state) {
        return Event.builder().id(1L).title("Event").annotation("Annotation").description("Description").initiatorId(1L)
                .initiatorName("User").categoryId(2L).categoryName("Category").createdOn(LocalDateTime.now())
                .eventDate(LocalDateTime.now().plusDays(1)).state(state).build();
    }
}
