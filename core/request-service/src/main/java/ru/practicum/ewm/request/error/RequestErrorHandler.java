package ru.practicum.ewm.request.error;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.error.*;
import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class RequestErrorHandler {
    @ExceptionHandler(NotFoundException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    RequestApiError notFound(NotFoundException e) { return error(e.getMessage(), "The required object was not found.", HttpStatus.NOT_FOUND); }
    @ExceptionHandler(ConflictException.class) @ResponseStatus(HttpStatus.CONFLICT)
    RequestApiError conflict(ConflictException e) { return error(e.getMessage(), "For the requested operation the conditions are not met.", HttpStatus.CONFLICT); }
    @ExceptionHandler(BadRequestException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    RequestApiError bad(BadRequestException e) { return error(e.getMessage(), "Incorrectly made request.", HttpStatus.BAD_REQUEST); }
    @ExceptionHandler(FeignException.class) @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    RequestApiError dependencyUnavailable(FeignException e) {
        return error("A dependent service is temporarily unavailable.", "Inter-service request failed.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
    private RequestApiError error(String message, String reason, HttpStatus status) {
        return RequestApiError.builder().errors(List.of()).message(message).reason(reason).status(status).timestamp(LocalDateTime.now()).build();
    }
}
