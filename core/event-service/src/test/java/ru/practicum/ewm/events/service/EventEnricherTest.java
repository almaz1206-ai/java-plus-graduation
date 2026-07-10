package ru.practicum.ewm.events.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.event.*;
import ru.practicum.interaction.request.*;
import ru.practicum.interaction.user.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventEnricherTest {
    @Mock UserContract users; @Mock CategoryContract categories; @Mock RequestContract requests;

    @Test void enrichesListWithOneBatchCallPerDependency() {
        when(users.getByIds(any())).thenReturn(new UsersResponse(List.of(new UserResponse(1L, "User", "u@example.com"))));
        when(categories.getByIds(any())).thenReturn(new CategoriesResponse(List.of(new CategoryResponse(2L, "Category"))));
        when(requests.getConfirmedCounts(any())).thenReturn(new ConfirmedRequestCountsResponse(List.of(
                new ConfirmedRequestCountResponse(10L, 3), new ConfirmedRequestCountResponse(11L, 4))));
        List<Event> events=List.of(event(10L), event(11L));
        new EventEnricher(users, categories, requests).enrich(events);
        assertThat(events).extracting(Event::getConfirmedRequests).containsExactly(3, 4);
        verify(users, times(1)).getByIds(any(IdsRequest.class));
        verify(categories, times(1)).getByIds(any(IdsRequest.class));
        verify(requests, times(1)).getConfirmedCounts(any(IdsRequest.class));
        verify(users, never()).getById(any());
        verify(categories, never()).getById(any());
    }
    private Event event(Long id) { return Event.builder().id(id).initiatorId(1L).categoryId(2L).build(); }
}
