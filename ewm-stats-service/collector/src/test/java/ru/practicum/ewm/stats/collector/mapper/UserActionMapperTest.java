package ru.practicum.ewm.stats.collector.mapper;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.service.collector.ActionTypeProto;
import ru.practicum.stats.service.collector.UserActionProto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserActionMapperTest {

    private final UserActionMapper mapper = new UserActionMapper();

    @ParameterizedTest
    @CsvSource({
            "ACTION_VIEW, VIEW",
            "ACTION_REGISTER, REGISTER",
            "ACTION_LIKE, LIKE"
    })
    void shouldMapEveryActionType(ActionTypeProto protoType, ActionTypeAvro avroType) {
        UserActionAvro result = mapper.map(action(protoType));

        assertEquals(avroType, result.getActionType());
    }

    @Test
    void shouldMapIdsAndRequestTimestamp() {
        UserActionAvro result = mapper.map(action(ActionTypeProto.ACTION_VIEW));

        assertEquals(11L, result.getUserId());
        assertEquals(22L, result.getEventId());
        assertEquals(1_700_000_000_123L, result.getTimestamp().toEpochMilli());
    }

    @Test
    void shouldRejectUnknownActionType() {
        UserActionProto source = UserActionProto.newBuilder(action(ActionTypeProto.ACTION_VIEW))
                .setActionTypeValue(99)
                .build();

        assertThrows(IllegalArgumentException.class, () -> mapper.map(source));
    }

    private UserActionProto action(ActionTypeProto actionType) {
        return UserActionProto.newBuilder()
                .setUserId(11L)
                .setEventId(22L)
                .setActionType(actionType)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(1_700_000_000L)
                        .setNanos(123_000_000)
                        .build())
                .build();
    }
}
