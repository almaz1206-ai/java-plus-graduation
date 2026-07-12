package ru.practicum.ewm.request.interaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.request.model.StatusRequest;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.interaction.common.IdsRequest;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestContractAdapterTest {
    @Mock
    RequestRepository repository;

    @Test
    void countsConfirmedRequestsWithSingleBatchQuery() {
        when(repository.countByEventIdsAndStatus(anyList(), eq(StatusRequest.CONFIRMED)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 2L}, new Object[]{2L, 3L}));
        var result = new RequestContractAdapter(repository).getConfirmedCounts(new IdsRequest(Set.of(1L, 2L, 3L)));
        assertThat(result.counts()).extracting("confirmedCount").containsExactlyInAnyOrder(2L, 3L, 0L);
        verify(repository, times(1)).countByEventIdsAndStatus(anyList(), eq(StatusRequest.CONFIRMED));
    }

    @Test
    void checksOnlyConfirmedParticipation() {
        when(repository.existsByRequesterIdAndEventIdAndStatus(10L, 20L, StatusRequest.CONFIRMED))
                .thenReturn(true);

        var result = new RequestContractAdapter(repository).hasConfirmedParticipation(10L, 20L);

        assertThat(result.exists()).isTrue();
        verify(repository).existsByRequesterIdAndEventIdAndStatus(10L, 20L, StatusRequest.CONFIRMED);
    }

    @Test
    void rejectedRequestIsNotConfirmedParticipation() {
        when(repository.existsByRequesterIdAndEventIdAndStatus(10L, 20L, StatusRequest.CONFIRMED))
                .thenReturn(false);

        assertThat(new RequestContractAdapter(repository).hasConfirmedParticipation(10L, 20L).exists()).isFalse();
    }
}
