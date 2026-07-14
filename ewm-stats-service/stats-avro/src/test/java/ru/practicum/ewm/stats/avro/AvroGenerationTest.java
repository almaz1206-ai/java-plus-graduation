package ru.practicum.ewm.stats.avro;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AvroGenerationTest {

    @Test
    void shouldCreateUserActionWithGeneratedEnum() {
        UserActionAvro action = UserActionAvro.newBuilder()
                .setUserId(10L)
                .setEventId(20L)
                .setActionType(ActionTypeAvro.LIKE)
                .setTimestamp(Instant.ofEpochMilli(1_700_000_000_123L))
                .build();

        assertEquals(10L, action.getUserId());
        assertEquals(20L, action.getEventId());
        assertEquals(ActionTypeAvro.LIKE, action.getActionType());
        assertEquals(1_700_000_000_123L, action.getTimestamp().toEpochMilli());
    }

    @Test
    void shouldCreateEventSimilarity() {
        EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                .setEventA(100L)
                .setEventB(200L)
                .setScore(0.75)
                .setTimestamp(Instant.ofEpochMilli(1_700_000_001_000L))
                .build();

        assertEquals(100L, similarity.getEventA());
        assertEquals(200L, similarity.getEventB());
        assertEquals(0.75, similarity.getScore());
        assertEquals(1_700_000_001_000L, similarity.getTimestamp().toEpochMilli());
    }

    @Test
    void shouldSerializeAndDeserializeSpecificRecords() throws IOException {
        UserActionAvro source = UserActionAvro.newBuilder()
                .setUserId(1L)
                .setEventId(2L)
                .setActionType(ActionTypeAvro.REGISTER)
                .setTimestamp(Instant.ofEpochMilli(3_000L))
                .build();

        UserActionAvro restored = roundTrip(source, UserActionAvro.getClassSchema());

        assertInstanceOf(UserActionAvro.class, restored);
        assertEquals(source, restored);
        assertEquals(ActionTypeAvro.REGISTER, restored.getActionType());
    }

    private static <T> T roundTrip(T source, Schema schema) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        SpecificDatumWriter<T> writer = new SpecificDatumWriter<>(schema);
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(output, null);
        writer.write(source, encoder);
        encoder.flush();

        SpecificDatumReader<T> reader = new SpecificDatumReader<>(schema);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(output.toByteArray(), null);
        return reader.read(null, decoder);
    }
}
