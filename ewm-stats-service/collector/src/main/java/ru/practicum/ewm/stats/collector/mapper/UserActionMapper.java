package ru.practicum.ewm.stats.collector.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.service.collector.ActionTypeProto;
import ru.practicum.stats.service.collector.UserActionProto;

import java.time.DateTimeException;
import java.time.Instant;

@Component
public class UserActionMapper {

    public UserActionAvro map(UserActionProto source) {
        Instant timestamp;
        try {
            timestamp = Instant.ofEpochSecond(source.getTimestamp().getSeconds(), source.getTimestamp().getNanos());
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Invalid action timestamp", exception);
        }

        return UserActionAvro.newBuilder()
                .setUserId(source.getUserId())
                .setEventId(source.getEventId())
                .setActionType(mapActionType(source.getActionType()))
                .setTimestamp(timestamp)
                .build();
    }

    private ActionTypeAvro mapActionType(ActionTypeProto actionType) {
        return switch (actionType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown action type");
        };
    }
}
