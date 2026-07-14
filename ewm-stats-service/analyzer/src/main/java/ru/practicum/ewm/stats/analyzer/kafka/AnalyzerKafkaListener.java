package ru.practicum.ewm.stats.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.analyzer.service.AnalyzerPersistenceService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerKafkaListener {

    private final AnalyzerPersistenceService persistenceService;

    @KafkaListener(
            topics = "${analyzer.kafka.user-actions-topic}",
            groupId = "${analyzer.kafka.user-actions-group}",
            containerFactory = "userActionKafkaListenerContainerFactory"
    )
    public void consumeUserAction(UserActionAvro action, Acknowledgment acknowledgment) {
        persistenceService.process(action);
        acknowledgment.acknowledge();
        log.debug("Stored user interaction for event={}", action.getEventId());
    }

    @KafkaListener(
            topics = "${analyzer.kafka.events-similarity-topic}",
            groupId = "${analyzer.kafka.events-similarity-group}",
            containerFactory = "eventSimilarityKafkaListenerContainerFactory"
    )
    public void consumeSimilarity(EventSimilarityAvro similarity, Acknowledgment acknowledgment) {
        persistenceService.process(similarity);
        acknowledgment.acknowledge();
        log.debug("Stored event similarity update");
    }
}
