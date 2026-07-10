package ru.practicum.ewm.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.user.error.UserErrorHandler;
import ru.practicum.ewm.user.service.UserService;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAdminControllerValidationTest {
    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new UserAdminController(mock(UserService.class)))
                .setControllerAdvice(new UserErrorHandler()).build();
        mvc.perform(post("/admin/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
