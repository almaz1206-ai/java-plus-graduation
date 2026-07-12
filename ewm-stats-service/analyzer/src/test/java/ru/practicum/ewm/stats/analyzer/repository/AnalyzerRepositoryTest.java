package ru.practicum.ewm.stats.analyzer.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.support.Acknowledgment;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarity;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityId;
import ru.practicum.ewm.stats.analyzer.model.UserInteraction;
import ru.practicum.ewm.stats.analyzer.model.UserInteractionId;
import ru.practicum.ewm.stats.analyzer.kafka.AnalyzerKafkaListener;
import ru.practicum.ewm.stats.analyzer.service.AnalyzerPersistenceService;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({AnalyzerPersistenceService.class, AnalyzerKafkaListener.class})
class AnalyzerRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.1");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private UserInteractionRepository interactionRepository;
    @Autowired
    private EventSimilarityRepository similarityRepository;
    @Autowired
    private AnalyzerKafkaListener kafkaListener;

    @BeforeEach
    void cleanDatabase() {
        similarityRepository.deleteAll();
        interactionRepository.deleteAll();
    }

    @Test
    void shouldKeepMaximumWeightAndTimestampOfActionThatRaisedIt() {
        Instant firstTimestamp = Instant.parse("2026-01-01T10:00:00Z");
        Instant ignoredTimestamp = Instant.parse("2026-01-02T10:00:00Z");
        Instant raisedTimestamp = Instant.parse("2026-01-03T10:00:00Z");

        assertEquals(1, interactionRepository.saveMaximumWeight(1L, 10L, 0.8, firstTimestamp));
        assertEquals(0, interactionRepository.saveMaximumWeight(1L, 10L, 0.4, ignoredTimestamp));
        assertEquals(0, interactionRepository.saveMaximumWeight(1L, 10L, 0.8, ignoredTimestamp));

        UserInteraction unchanged = interactionRepository.findById(new UserInteractionId(1L, 10L)).orElseThrow();
        assertEquals(0.8, unchanged.getWeight());
        assertEquals(firstTimestamp, unchanged.getLastActionTimestamp());

        assertEquals(1, interactionRepository.saveMaximumWeight(1L, 10L, 1.0, raisedTimestamp));
        UserInteraction raised = interactionRepository.findById(new UserInteractionId(1L, 10L)).orElseThrow();
        assertEquals(1.0, raised.getWeight());
        assertEquals(raisedTimestamp, raised.getLastActionTimestamp());
    }

    @Test
    void shouldFindUserInteractionsAndWeightSum() {
        interactionRepository.saveMaximumWeight(1L, 10L, 0.4, Instant.parse("2026-01-01T10:00:00Z"));
        interactionRepository.saveMaximumWeight(1L, 20L, 1.0, Instant.parse("2026-01-02T10:00:00Z"));
        interactionRepository.saveMaximumWeight(2L, 10L, 0.8, Instant.parse("2026-01-03T10:00:00Z"));

        List<UserInteraction> interactions = interactionRepository
                .findAllByIdUserIdOrderByLastActionTimestampDesc(1L);

        assertEquals(List.of(20L, 10L), interactions.stream()
                .map(value -> value.getId().getEventId()).toList());
        assertTrue(interactionRepository.existsByIdUserIdAndIdEventId(1L, 10L));
        assertFalse(interactionRepository.existsByIdUserIdAndIdEventId(2L, 20L));
        assertEquals(1.2, interactionRepository.sumWeightsByEventId(10L), 1.0e-9);
        assertEquals(List.of(10L, 20L), interactionRepository.sumWeightsByEventIds(List.of(10L, 20L)).stream()
                .map(EventWeightSumView::getEventId).sorted().toList());
    }

    @Test
    void shouldFindSimilarityOnEitherSideAndOrderSimilarEvents() {
        similarityRepository.save(similarity(10L, 20L, 0.7));
        similarityRepository.save(similarity(5L, 10L, 0.9));
        similarityRepository.save(similarity(20L, 30L, 0.5));

        assertEquals(2, similarityRepository.findAllByIdEventAOrIdEventB(10L, 10L).size());
        List<SimilarEventView> similar = similarityRepository.findSimilarEvents(10L, PageRequest.of(0, 10));
        assertEquals(List.of(5L, 20L), similar.stream().map(SimilarEventView::getEventId).toList());
        assertEquals(List.of(0.9, 0.7), similar.stream().map(SimilarEventView::getScore).toList());
    }

    @Test
    void shouldFindOnlyNeighborsAlreadyViewedByUser() {
        similarityRepository.save(similarity(10L, 20L, 0.7));
        similarityRepository.save(similarity(10L, 30L, 0.9));
        interactionRepository.saveMaximumWeight(1L, 20L, 0.4, Instant.EPOCH);

        List<SimilarEventView> neighbors = similarityRepository
                .findNeighborsViewedByUser(10L, 1L, PageRequest.of(0, 10));

        assertEquals(List.of(20L), neighbors.stream().map(SimilarEventView::getEventId).toList());
    }

    @Test
    void shouldRejectNonNormalizedSimilarityPair() {
        similarityRepository.saveAndFlush(similarity(10L, 20L, 0.7));

        assertThrows(DataIntegrityViolationException.class,
                () -> similarityRepository.saveAndFlush(similarity(20L, 10L, 0.7)));
    }

    @Test
    void shouldUpsertSimilarityWithoutCreatingReverseDuplicate() {
        Instant firstTimestamp = Instant.parse("2026-01-01T10:00:00Z");
        Instant secondTimestamp = Instant.parse("2026-01-02T10:00:00Z");

        assertEquals(1, similarityRepository.upsert(10L, 20L, 0.5, firstTimestamp));
        assertEquals(1, similarityRepository.upsert(10L, 20L, 0.9, secondTimestamp));

        assertEquals(1, similarityRepository.count());
        EventSimilarity stored = similarityRepository.findById(new EventSimilarityId(10L, 20L)).orElseThrow();
        assertEquals(0.9, stored.getScore());
        assertEquals(secondTimestamp, stored.getUpdatedAt());
        assertFalse(similarityRepository.existsById(new EventSimilarityId(20L, 10L)));
    }

    @Test
    void shouldConsumeBothAvroMessagesIdempotentlyAndAcknowledgeAfterDatabaseWrite() {
        Acknowledgment actionAck = mock(Acknowledgment.class);
        UserActionAvro action = UserActionAvro.newBuilder()
                .setUserId(7L).setEventId(70L).setActionType(ActionTypeAvro.REGISTER)
                .setTimestamp(Instant.parse("2026-07-12T10:00:00Z")).build();

        kafkaListener.consumeUserAction(action, actionAck);
        kafkaListener.consumeUserAction(action, actionAck);

        assertEquals(1, interactionRepository.count());
        UserInteraction interaction = interactionRepository
                .findById(new UserInteractionId(7L, 70L)).orElseThrow();
        assertEquals(0.8, interaction.getWeight(), 1.0e-9);
        assertEquals(action.getTimestamp(), interaction.getLastActionTimestamp());
        verify(actionAck, times(2)).acknowledge();

        Acknowledgment similarityAck = mock(Acknowledgment.class);
        EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                .setEventA(90L).setEventB(70L).setScore(0.75)
                .setTimestamp(Instant.parse("2026-07-12T11:00:00Z")).build();

        kafkaListener.consumeSimilarity(similarity, similarityAck);
        kafkaListener.consumeSimilarity(similarity, similarityAck);

        assertEquals(1, similarityRepository.count());
        EventSimilarity stored = similarityRepository
                .findById(new EventSimilarityId(70L, 90L)).orElseThrow();
        assertEquals(0.75, stored.getScore(), 1.0e-9);
        assertEquals(similarity.getTimestamp(), stored.getUpdatedAt());
        assertFalse(similarityRepository.existsById(new EventSimilarityId(90L, 70L)));
        verify(similarityAck, times(2)).acknowledge();
    }

    private EventSimilarity similarity(long eventA, long eventB, double score) {
        return new EventSimilarity(new EventSimilarityId(eventA, eventB), score, Instant.now());
    }
}
