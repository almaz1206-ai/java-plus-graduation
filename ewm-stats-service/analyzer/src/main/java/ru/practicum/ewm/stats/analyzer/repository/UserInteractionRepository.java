package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.model.UserInteraction;
import ru.practicum.ewm.stats.analyzer.model.UserInteractionId;

import java.time.Instant;
import java.util.List;

public interface UserInteractionRepository extends JpaRepository<UserInteraction, UserInteractionId> {

    List<UserInteraction> findAllByIdUserIdOrderByLastActionTimestampDesc(Long userId);

    boolean existsByIdUserIdAndIdEventId(Long userId, Long eventId);

    @Query("SELECT COALESCE(SUM(interaction.weight), 0.0) FROM UserInteraction interaction "
            + "WHERE interaction.id.eventId = :eventId")
    double sumWeightsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT interaction.id.eventId AS eventId, SUM(interaction.weight) AS weightSum "
            + "FROM UserInteraction interaction WHERE interaction.id.eventId IN :eventIds "
            + "GROUP BY interaction.id.eventId")
    List<EventWeightSumView> sumWeightsByEventIds(@Param("eventIds") List<Long> eventIds);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO user_interactions (user_id, event_id, weight, last_action_timestamp)
            VALUES (:userId, :eventId, :weight, :timestamp)
            ON CONFLICT (user_id, event_id) DO UPDATE
            SET weight = EXCLUDED.weight,
                last_action_timestamp = EXCLUDED.last_action_timestamp
            WHERE EXCLUDED.weight > user_interactions.weight
            """, nativeQuery = true)
    int saveMaximumWeight(@Param("userId") Long userId,
                          @Param("eventId") Long eventId,
                          @Param("weight") double weight,
                          @Param("timestamp") Instant timestamp);
}
