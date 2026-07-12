package ru.practicum.ewm.stats.avro.serialization;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroSerializer<T extends SpecificRecord> implements Serializer<T> {

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            SpecificDatumWriter<T> writer = new SpecificDatumWriter<>(data.getSchema());
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(output, null);
            writer.write(data, encoder);
            encoder.flush();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new SerializationException("Failed to serialize Avro record", exception);
        }
    }
}
