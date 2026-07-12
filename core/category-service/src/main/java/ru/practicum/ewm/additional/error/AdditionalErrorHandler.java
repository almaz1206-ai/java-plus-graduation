package ru.practicum.ewm.additional.error;

import feign.FeignException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.error.ConflictException;
import ru.practicum.ewm.error.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class AdditionalErrorHandler {
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    AdditionalApiError notFound(NotFoundException e) {
        return error(e.getMessage(), "The required object was not found.", HttpStatus.NOT_FOUND, List.of());
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    AdditionalApiError conflict(Exception e) {
        return error(e.getMessage(), "For the requested operation the conditions are not met.", HttpStatus.CONFLICT, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    AdditionalApiError validation(MethodArgumentNotValidException e) {
        return error("Incorrectly made request.", "Incorrectly made request.", HttpStatus.BAD_REQUEST,
                e.getBindingResult().getFieldErrors().stream().map(f -> f.getField() + ": " + f.getDefaultMessage()).toList());
    }

    @ExceptionHandler(FeignException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    AdditionalApiError dependencyUnavailable(FeignException e) {
        return error("A dependent service is temporarily unavailable.", "Inter-service request failed.",
                HttpStatus.SERVICE_UNAVAILABLE, List.of());
    }

    private AdditionalApiError error(String message, String reason, HttpStatus status, List<String> errors) {
        return AdditionalApiError.builder().errors(errors).message(message).reason(reason).status(status).timestamp(LocalDateTime.now()).build();
    }
}
