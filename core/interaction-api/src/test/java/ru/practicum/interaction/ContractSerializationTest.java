package ru.practicum.interaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.event.CategoryResponse;
import ru.practicum.interaction.event.EventSummaryResponse;
import ru.practicum.interaction.request.RequestStatus;
import ru.practicum.interaction.request.RequestStatusResponse;
import ru.practicum.interaction.user.UserResponse;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ContractSerializationTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void shouldRoundTripCoreContracts() throws Exception {
        assertThat(roundTrip(new UserResponse(1L, "User", "u@example.com"), UserResponse.class))
                .isEqualTo(new UserResponse(1L, "User", "u@example.com"));
        assertThat(roundTrip(new IdsRequest(Set.of(1L, 2L)), IdsRequest.class).ids())
                .containsExactlyInAnyOrder(1L, 2L);
        EventSummaryResponse event = new EventSummaryResponse(2L, "Title", "Annotation", 1L, "User",
                new CategoryResponse(3L, "Category"), false, LocalDateTime.of(2030, 1, 1, 10, 0), 4, 5L);
        assertThat(roundTrip(event, EventSummaryResponse.class)).isEqualTo(event);
        RequestStatusResponse status = new RequestStatusResponse(4L, RequestStatus.CONFIRMED);
        assertThat(roundTrip(status, RequestStatusResponse.class)).isEqualTo(status);
    }

    private <T> T roundTrip(T value, Class<T> type) throws Exception {
        return mapper.readValue(mapper.writeValueAsBytes(value), type);
    }
}
