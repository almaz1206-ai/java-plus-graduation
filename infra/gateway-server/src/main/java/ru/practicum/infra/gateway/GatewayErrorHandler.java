package ru.practicum.infra.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {
    private final ObjectMapper objectMapper;

    public GatewayErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        HttpStatus status = resolveStatus(error);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "errors", List.of(),
                "message", message(status),
                "reason", status.is5xxServerError()
                        ? "Gateway could not complete the request."
                        : "Gateway request could not be resolved.",
                "status", status.name(),
                "timestamp", LocalDateTime.now().toString()
        );
        try {
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(objectMapper.writeValueAsBytes(body));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException exception) {
            return exchange.getResponse().setComplete();
        }
    }

    private HttpStatus resolveStatus(Throwable error) {
        if (error instanceof ResponseStatusException responseStatusException
                && responseStatusException.getStatusCode() instanceof HttpStatus status) {
            return status;
        }
        return isTimeout(error) ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.SERVICE_UNAVAILABLE;
    }

    private String message(HttpStatus status) {
        if (status == HttpStatus.GATEWAY_TIMEOUT) return "Upstream service response timed out.";
        if (status == HttpStatus.SERVICE_UNAVAILABLE) return "Upstream service is temporarily unavailable.";
        return status.getReasonPhrase();
    }

    private boolean isTimeout(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof TimeoutException) return true;
            current = current.getCause();
        }
        return false;
    }
}
