package ru.practicum.ewm.stats.avro.support;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;

public enum ActionWeight {
    VIEW(0.4),
    REGISTER(0.8),
    LIKE(1.0);

    private final double value;

    ActionWeight(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public static double from(ActionTypeAvro actionType) {
        return ActionWeight.valueOf(actionType.name()).getValue();
    }
}
