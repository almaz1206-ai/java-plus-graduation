package ru.practicum.ewm.user.interaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;
import ru.practicum.interaction.common.IdsRequest;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserContractAdapterTest {
    @Mock UserRepository repository;

    @Test
    void getsUsersByIdsWithSingleRepositoryCall() {
        User first = user(1L); User second = user(2L);
        when(repository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(first, second));
        var result = new UserContractAdapter(repository).getByIds(new IdsRequest(Set.of(1L, 2L)));
        assertThat(result.users()).extracting("id").containsExactlyInAnyOrder(1L, 2L);
        verify(repository).findAllById(Set.of(1L, 2L));
    }

    private User user(Long id) {
        User user = new User(); user.setId(id); user.setName("User " + id); user.setEmail("u" + id + "@example.com"); return user;
    }
}
