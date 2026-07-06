package ru.practicum.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ErrorHandler {

    /**
     * Handles Bean Validation failures (@Valid on request body).
     * Returns 400 with a map of { fieldName -> errorMessage }.
     */
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationException(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid value"
                ));
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(BadRequestException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMissingParam(MissingServletRequestParameterException ex) {
        return Map.of("error", "Required parameter '" + ex.getParameterName() + "' is missing");
    }

    /**
     * Handles malformed timestamp strings that pass regex but still fail parsing
     * (e.g. month=13), or any direct service usage that bypasses @Valid.
     * Returns 400 with a descriptive message.
     */
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleDateTimeParseException(DateTimeParseException ex) {
        return Map.of(
                "timestamp", "invalid date-time value: " + ex.getParsedString()
                        + " — expected format yyyy-MM-dd HH:mm:ss"
        );
    }
}
