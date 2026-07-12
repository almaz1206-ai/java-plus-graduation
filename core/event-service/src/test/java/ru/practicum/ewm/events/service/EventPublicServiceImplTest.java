package ru.practicum.ewm.events.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.events.interaction.EventStatsFeignClient;
import ru.practicum.ewm.events.model.*;
import ru.practicum.ewm.events.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventPublicServiceImplTest {
    @Mock
    EventRepository repository;
    @Mock
    EventStatsFeignClient stats;
    @Mock
    EventEnricher enricher;
    @Mock
    HttpServletRequest request;
    @InjectMocks
    EventPublicServiceImpl service;

    @Test
    void performsPublicSearch() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(event())));
        when(request.getRequestURI()).thenReturn("/events");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(stats.getStats(any(), any(), anyList(), eq(true))).thenReturn(List.of());
        assertThat(service.getPublicEvents(null, null, null, null, null, false, null, 0, 10, request)).hasSize(1);
    }

    private Event event() {
        return Event.builder().id(1L).title("Event").annotation("Annotation").description("Description")
                .initiatorId(1L).initiatorName("User").categoryId(2L).categoryName("Category")
                .createdOn(LocalDateTime.now()).eventDate(LocalDateTime.now().plusDays(1)).state(EventState.PUBLISHED).build();
    }
}
