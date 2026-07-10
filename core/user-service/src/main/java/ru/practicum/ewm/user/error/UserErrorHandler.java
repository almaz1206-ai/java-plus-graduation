package ru.practicum.ewm.user.error;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.ewm.error.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class UserErrorHandler {
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public UserApiError notFound(NotFoundException exception) {
        return error(exception.getMessage(), "The required object was not found.", HttpStatus.NOT_FOUND, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public UserApiError conflict(DataIntegrityViolationException exception) {
        return error("Integrity constraint has been violated.",
                "Integrity constraint has been violated.", HttpStatus.CONFLICT, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public UserApiError validation(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage()).toList();
        return error("Incorrectly made request.", "Incorrectly made request.", HttpStatus.BAD_REQUEST, errors);
    }

    private UserApiError error(String message, String reason, HttpStatus status, List<String> errors) {
        return UserApiError.builder().errors(errors).message(message).reason(reason).status(status)
                .timestamp(LocalDateTime.now()).build();
    }
}
