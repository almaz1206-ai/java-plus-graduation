package ru.practicum.ewm.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.support.ActionWeight;

@Service
@RequiredArgsConstructor
public class AnalyzerPersistenceService {

    private final UserInteractionRepository interactionRepository;
    private final EventSimilarityRepository similarityRepository;

    @Transactional
    public void process(UserActionAvro action) {
        interactionRepository.saveMaximumWeight(
                action.getUserId(),
                action.getEventId(),
                ActionWeight.from(action.getActionType()),
                action.getTimestamp());
    }

    @Transactional
    public void process(EventSimilarityAvro similarity) {
        long first = Math.min(similarity.getEventA(), similarity.getEventB());
        long second = Math.max(similarity.getEventA(), similarity.getEventB());
        if (first == second) {
            throw new IllegalArgumentException("Similarity pair must contain distinct events");
        }
        similarityRepository.upsert(first, second, similarity.getScore(), similarity.getTimestamp());
    }
}
