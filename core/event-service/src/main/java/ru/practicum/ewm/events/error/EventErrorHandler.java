package ru.practicum.ewm.events.error;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.error.*;
import ru.practicum.ewm.stats.client.StatsClientException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class EventErrorHandler {
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    EventApiError notFound(NotFoundException e) {
        return error(e.getMessage(), "The required object was not found.", HttpStatus.NOT_FOUND, List.of());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    EventApiError conflict(ConflictException e) {
        return error(e.getMessage(), "For the requested operation the conditions are not met.", HttpStatus.CONFLICT, List.of());
    }

    @ExceptionHandler({BadRequestException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    EventApiError badRequest(Exception e) {
        return error(e.getMessage(), "Incorrectly made request.", HttpStatus.BAD_REQUEST, List.of());
    }

    @ExceptionHandler(FeignException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    EventApiError dependencyUnavailable(FeignException e) {
        return error("A dependent service is temporarily unavailable.", "Inter-service request failed.",
                HttpStatus.SERVICE_UNAVAILABLE, List.of());
    }

    @ExceptionHandler(StatsClientException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    EventApiError statsUnavailable(StatsClientException e) {
        return error("The recommendation service is temporarily unavailable.", "Inter-service request failed.",
                HttpStatus.SERVICE_UNAVAILABLE, List.of());
    }

    private EventApiError error(String message, String reason, HttpStatus status, List<String> errors) {
        return EventApiError.builder().errors(errors).message(message).reason(reason).status(status).timestamp(LocalDateTime.now()).build();
    }
}
