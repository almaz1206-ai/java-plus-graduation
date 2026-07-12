package ru.practicum.ewm.stats.aggregator.service;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.aggregator.model.EventPair;
import ru.practicum.ewm.stats.avro.support.ActionWeight;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SimilarityState {

    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();
    private final Map<Long, Double> eventWeightSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightSums = new HashMap<>();

    public synchronized void process(UserActionAvro action, SimilarityPublisher publisher) throws Exception {
        Map<Long, Map<Long, Double>> weightsSnapshot = copyNestedMap(eventUserWeights);
        Map<Long, Double> sumsSnapshot = new HashMap<>(eventWeightSums);
        Map<Long, Map<Long, Double>> minSumsSnapshot = copyNestedMap(minWeightSums);
        try {
            publisher.publish(update(action));
        } catch (Exception exception) {
            restore(eventUserWeights, weightsSnapshot);
            eventWeightSums.clear();
            eventWeightSums.putAll(sumsSnapshot);
            restore(minWeightSums, minSumsSnapshot);
            throw exception;
        }
    }

    public synchronized List<EventSimilarityAvro> update(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();
        double newWeight = ActionWeight.from(action.getActionType());
        Map<Long, Double> userWeights = eventUserWeights.computeIfAbsent(eventId, ignored -> new HashMap<>());
        double oldWeight = userWeights.getOrDefault(userId, 0.0);
        if (newWeight <= oldWeight) {
            return List.of();
        }

        userWeights.put(userId, newWeight);
        eventWeightSums.merge(eventId, newWeight - oldWeight, Double::sum);

        Set<EventPair> affectedPairs = new HashSet<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeights.entrySet()) {
            long otherEventId = entry.getKey();
            if (otherEventId == eventId) {
                continue;
            }
            Double otherWeight = entry.getValue().get(userId);
            if (otherWeight == null) {
                continue;
            }
            EventPair pair = EventPair.of(eventId, otherEventId);
            double minDelta = Math.min(newWeight, otherWeight) - Math.min(oldWeight, otherWeight);
            minWeightSums.computeIfAbsent(pair.first(), ignored -> new HashMap<>())
                    .merge(pair.second(), minDelta, Double::sum);
            affectedPairs.add(pair);
        }

        List<EventSimilarityAvro> updates = new ArrayList<>(affectedPairs.size());
        for (EventPair pair : affectedPairs) {
            double denominator = Math.sqrt(eventWeightSums.getOrDefault(pair.first(), 0.0)
                    * eventWeightSums.getOrDefault(pair.second(), 0.0));
            if (denominator == 0.0) {
                continue;
            }
            double minSum = minWeightSums.getOrDefault(pair.first(), Map.of())
                    .getOrDefault(pair.second(), 0.0);
            updates.add(EventSimilarityAvro.newBuilder()
                    .setEventA(pair.first())
                    .setEventB(pair.second())
                    .setScore(minSum / denominator)
                    .setTimestamp(action.getTimestamp())
                    .build());
        }
        updates.sort(Comparator.comparingLong(EventSimilarityAvro::getEventA)
                .thenComparingLong(EventSimilarityAvro::getEventB));
        return updates;
    }

    private Map<Long, Map<Long, Double>> copyNestedMap(Map<Long, Map<Long, Double>> source) {
        Map<Long, Map<Long, Double>> copy = new HashMap<>();
        source.forEach((key, value) -> copy.put(key, new HashMap<>(value)));
        return copy;
    }

    private void restore(Map<Long, Map<Long, Double>> target, Map<Long, Map<Long, Double>> snapshot) {
        target.clear();
        snapshot.forEach((key, value) -> target.put(key, new HashMap<>(value)));
    }

    @FunctionalInterface
    public interface SimilarityPublisher {
        void publish(List<EventSimilarityAvro> similarities) throws Exception;
    }
}
