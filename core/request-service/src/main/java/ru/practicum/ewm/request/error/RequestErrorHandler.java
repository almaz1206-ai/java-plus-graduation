package ru.practicum.ewm.request.error;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.error.*;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class RequestErrorHandler {
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    RequestApiError notFound(NotFoundException e) {
        return error(e.getMessage(), "The required object was not found.", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    RequestApiError conflict(ConflictException e) {
        return error(e.getMessage(), "For the requested operation the conditions are not met.", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    RequestApiError bad(BadRequestException e) {
        return error(e.getMessage(), "Incorrectly made request.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    RequestApiError validation(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return RequestApiError.builder()
                .errors(errors)
                .message("Validation failed.")
                .reason("Incorrectly made request.")
                .status(HttpStatus.BAD_REQUEST)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(FeignException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    RequestApiError dependencyUnavailable(FeignException e) {
        return error("A dependent service is temporarily unavailable.", "Inter-service request failed.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    private RequestApiError error(String message, String reason, HttpStatus status) {
        return RequestApiError.builder().errors(List.of()).message(message).reason(reason).status(status).timestamp(LocalDateTime.now()).build();
    }
}
