package ru.practicum.ewm.stats.aggregator.model;

public record EventPair(long first, long second) {

    public EventPair {
        if (first >= second) {
            throw new IllegalArgumentException("Event pair must be normalized and contain distinct events");
        }
    }

    public static EventPair of(long eventA, long eventB) {
        if (eventA == eventB) {
            throw new IllegalArgumentException("An event cannot be paired with itself");
        }
        return new EventPair(Math.min(eventA, eventB), Math.max(eventA, eventB));
    }

    public String kafkaKey() {
        return first + ":" + second;
    }
}
