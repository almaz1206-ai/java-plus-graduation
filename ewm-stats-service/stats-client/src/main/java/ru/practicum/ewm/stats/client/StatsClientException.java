package ru.practicum.ewm.stats.client;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class StatsClientException extends RuntimeException {
    private final Status.Code statusCode;

    public StatsClientException(String operation, StatusRuntimeException cause) {
        super("gRPC call failed: " + operation + ", status=" + cause.getStatus().getCode(), cause);
        statusCode = cause.getStatus().getCode();
    }

    public Status.Code getStatusCode() {
        return statusCode;
    }
}
