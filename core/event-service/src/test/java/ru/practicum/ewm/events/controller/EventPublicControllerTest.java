package ru.practicum.ewm.events.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.events.dto.EventFullDto;
import ru.practicum.ewm.events.error.EventErrorHandler;
import ru.practicum.ewm.events.service.EventPublicService;
import ru.practicum.ewm.error.BadRequestException;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventPublicControllerTest {
    private final EventPublicService service = mock(EventPublicService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new EventPublicController(service))
                .setControllerAdvice(new EventErrorHandler())
                .build();
    }

    @Test
    void requiresUserHeaderForSinglePublicEvent() throws Exception {
        mvc.perform(get("/events/1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void passesHeaderUserIdToService() throws Exception {
        when(service.getPublicEventById(1L, 25L)).thenReturn(EventFullDto.builder().id(1L).build());

        mvc.perform(get("/events/1").header("X-EWM-USER-ID", "25"))
                .andExpect(status().isOk());

        verify(service).getPublicEventById(1L, 25L);
    }

    @Test
    void requiresUserHeaderForRecommendations() throws Exception {
        mvc.perform(get("/events/recommendations"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passesRecommendationHeaderAndLimitToService() throws Exception {
        when(service.getRecommendations(25L, 7)).thenReturn(List.of());

        mvc.perform(get("/events/recommendations")
                        .header("X-EWM-USER-ID", "25")
                        .param("size", "7"))
                .andExpect(status().isOk());

        verify(service).getRecommendations(25L, 7);
    }

    @Test
    void returnsBadRequestWhenUserCannotLikeEvent() throws Exception {
        doThrow(new BadRequestException("not visited"))
                .when(service).likeEvent(7L, 25L);

        mvc.perform(put("/events/7/like").header("X-EWM-USER-ID", "25"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresUserHeaderForLike() throws Exception {
        mvc.perform(put("/events/7/like"))
                .andExpect(status().isBadRequest());
    }
}
