package ru.practicum.ewm.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    UserRepository repository;
    UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(repository);
    }

    @Test
    void createsUser() {
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        var result = service.addUser(new NewUserRequest("user@example.com", "User"));
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void getsUsers() {
        User user = user(1L);
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));
        assertThat(service.getUsers(null, 0, 10)).extracting("id").containsExactly(1L);
    }

    @Test
    void deletesExistingUser() {
        when(repository.existsById(1L)).thenReturn(true);
        service.deleteUser(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void reportsMissingUser() {
        when(repository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.deleteUser(99L)).isInstanceOf(NotFoundException.class);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("User");
        user.setEmail("user@example.com");
        return user;
    }
}
