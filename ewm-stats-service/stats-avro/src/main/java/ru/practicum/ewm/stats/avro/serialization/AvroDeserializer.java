package ru.practicum.ewm.stats.avro.serialization;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;

public class AvroDeserializer<T extends SpecificRecord> implements Deserializer<T> {

    private final SpecificDatumReader<T> reader;

    public AvroDeserializer(Class<T> targetType) {
        this.reader = new SpecificDatumReader<>(targetType);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            return reader.read(null, decoder);
        } catch (IOException exception) {
            throw new SerializationException("Failed to deserialize Avro record", exception);
        }
    }
}
