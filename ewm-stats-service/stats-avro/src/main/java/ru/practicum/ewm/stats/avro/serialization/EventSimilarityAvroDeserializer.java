package ru.practicum.ewm.stats.avro.serialization;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public class EventSimilarityAvroDeserializer extends AvroDeserializer<EventSimilarityAvro> {

    public EventSimilarityAvroDeserializer() {
        super(EventSimilarityAvro.class);
    }
}
