package ru.practicum.ewm.stats.analyzer.grpc;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.UserInteractionRepository;
import ru.practicum.stats.service.dashboard.InteractionsCountRequestProto;
import ru.practicum.stats.service.dashboard.RecommendedEventProto;
import ru.practicum.stats.service.dashboard.SimilarEventsRequestProto;
import ru.practicum.stats.service.dashboard.UserPredictionsRequestProto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "analyzer.recommendations.history-limit=50",
        "analyzer.recommendations.candidate-limit=200",
        "analyzer.recommendations.neighbor-limit=10"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RecommendationsGrpcService.class)
@Testcontainers(disabledWithoutDocker = true)
class RecommendationsGrpcServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.1");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private RecommendationsGrpcService service;
    @Autowired
    private EventSimilarityRepository similarityRepository;
    @Autowired
    private UserInteractionRepository interactionRepository;

    @BeforeEach
    void cleanDatabase() {
        similarityRepository.deleteAll();
        interactionRepository.deleteAll();
    }

    @Test
    void shouldFindBothSidesExcludeViewedSortAndLimit() {
        similarityRepository.upsert(5L, 10L, 0.8, Instant.EPOCH);
        similarityRepository.upsert(10L, 20L, 0.9, Instant.EPOCH);
        similarityRepository.upsert(10L, 30L, 0.8, Instant.EPOCH);
        interactionRepository.saveMaximumWeight(1L, 20L, 0.4, Instant.EPOCH);
        RecordingObserver observer = new RecordingObserver();

        service.getSimilarEvents(SimilarEventsRequestProto.newBuilder()
                .setEventId(10L).setUserId(1L).setMaxResults(2).build(), observer);

        assertEquals(List.of(5L, 30L), observer.values.stream()
                .map(RecommendedEventProto::getEventId).toList());
        assertTrue(observer.completed);
    }

    @Test
    void shouldSumSeveralUsersAndReturnZeroForMissingEvent() {
        interactionRepository.saveMaximumWeight(1L, 10L, 0.4, Instant.EPOCH);
        interactionRepository.saveMaximumWeight(2L, 10L, 1.0, Instant.EPOCH);
        RecordingObserver observer = new RecordingObserver();

        service.getInteractionsCount(InteractionsCountRequestProto.newBuilder()
                .addEventIds(10L).addEventIds(99L).build(), observer);

        assertEquals(List.of(1.4, 0.0), observer.values.stream()
                .map(RecommendedEventProto::getScore).toList());
        assertTrue(observer.completed);
    }

    @Test
    void shouldBuildPredictionsFromSeveralRecentInteractionsWithoutReturningViewedEvents() {
        interactionRepository.saveMaximumWeight(1L, 1L, 0.4, Instant.parse("2026-01-02T00:00:00Z"));
        interactionRepository.saveMaximumWeight(1L, 2L, 1.0, Instant.parse("2026-01-01T00:00:00Z"));
        similarityRepository.upsert(1L, 100L, 0.9, Instant.EPOCH);
        similarityRepository.upsert(2L, 100L, 0.3, Instant.EPOCH);
        similarityRepository.upsert(1L, 200L, 0.7, Instant.EPOCH);
        similarityRepository.upsert(2L, 200L, 0.2, Instant.EPOCH);
        similarityRepository.upsert(1L, 2L, 1.0, Instant.EPOCH);
        RecordingObserver observer = new RecordingObserver();

        service.getRecommendationsForUser(UserPredictionsRequestProto.newBuilder()
                .setUserId(1L).setMaxResults(2).build(), observer);

        assertEquals(List.of(100L, 200L), observer.values.stream()
                .map(RecommendedEventProto::getEventId).toList());
        assertEquals((0.4 * 0.9 + 1.0 * 0.3) / 1.2, observer.values.get(0).getScore(), 1.0e-9);
        assertEquals((0.4 * 0.7 + 1.0 * 0.2) / 0.9, observer.values.get(1).getScore(), 1.0e-9);
        assertTrue(observer.values.stream().noneMatch(value -> value.getEventId() == 1L || value.getEventId() == 2L));
        assertTrue(observer.completed);
    }

    @Test
    void shouldReturnEmptyHistoryAndApplyStableOrderAndLimit() {
        RecordingObserver empty = new RecordingObserver();
        service.getRecommendationsForUser(UserPredictionsRequestProto.newBuilder()
                .setUserId(99L).setMaxResults(3).build(), empty);
        assertTrue(empty.values.isEmpty());
        assertTrue(empty.completed);

        interactionRepository.saveMaximumWeight(1L, 1L, 1.0, Instant.EPOCH);
        similarityRepository.upsert(1L, 30L, 0.8, Instant.EPOCH);
        similarityRepository.upsert(1L, 20L, 0.8, Instant.EPOCH);
        similarityRepository.upsert(1L, 10L, 0.8, Instant.EPOCH);
        RecordingObserver limited = new RecordingObserver();

        service.getRecommendationsForUser(UserPredictionsRequestProto.newBuilder()
                .setUserId(1L).setMaxResults(2).build(), limited);

        assertEquals(List.of(10L, 20L), limited.values.stream()
                .map(RecommendedEventProto::getEventId).toList());
        assertTrue(limited.values.stream().noneMatch(value -> value.getEventId() == 1L));
        assertTrue(limited.completed);
    }

    private static class RecordingObserver implements StreamObserver<RecommendedEventProto> {
        private final List<RecommendedEventProto> values = new ArrayList<>();
        private boolean completed;

        @Override
        public void onNext(RecommendedEventProto value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable throwable) {
            throw new AssertionError(throwable);
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }
}
