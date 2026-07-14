package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarity;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityId;

import java.util.List;
import java.time.Instant;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, EventSimilarityId> {

    @Query(value = """
            WITH recent_interactions AS (
                SELECT interaction.event_id
                FROM user_interactions interaction
                WHERE interaction.user_id = :userId
                ORDER BY interaction.last_action_timestamp DESC, interaction.event_id ASC
                LIMIT :historyLimit
            ),
            viewed_events AS (
                SELECT interaction.event_id, interaction.weight
                FROM user_interactions interaction
                WHERE interaction.user_id = :userId
            ),
            candidate_scores AS (
                SELECT CASE WHEN similarity.event_a = recent.event_id
                            THEN similarity.event_b ELSE similarity.event_a END AS candidate_id,
                       MAX(similarity.score) AS preliminary_score
                FROM recent_interactions recent
                JOIN event_similarities similarity
                  ON similarity.event_a = recent.event_id OR similarity.event_b = recent.event_id
                WHERE CASE WHEN similarity.event_a = recent.event_id
                           THEN similarity.event_b ELSE similarity.event_a END <> recent.event_id
                  AND NOT EXISTS (
                      SELECT 1 FROM viewed_events viewed
                      WHERE viewed.event_id = CASE WHEN similarity.event_a = recent.event_id
                                                   THEN similarity.event_b ELSE similarity.event_a END
                  )
                GROUP BY candidate_id
                ORDER BY preliminary_score DESC, candidate_id ASC
                LIMIT :candidateLimit
            ),
            ranked_neighbors AS (
                SELECT candidate.candidate_id,
                       viewed.event_id AS neighbor_id,
                       viewed.weight AS user_weight,
                       similarity.score,
                       ROW_NUMBER() OVER (
                           PARTITION BY candidate.candidate_id
                           ORDER BY similarity.score DESC, viewed.event_id ASC
                       ) AS neighbor_rank
                FROM candidate_scores candidate
                JOIN event_similarities similarity
                  ON similarity.event_a = candidate.candidate_id
                  OR similarity.event_b = candidate.candidate_id
                JOIN viewed_events viewed
                  ON viewed.event_id = CASE WHEN similarity.event_a = candidate.candidate_id
                                            THEN similarity.event_b ELSE similarity.event_a END
                WHERE viewed.event_id <> candidate.candidate_id
            )
            SELECT candidate_id AS "candidateId",
                   neighbor_id AS "neighborId",
                   user_weight AS "userWeight",
                   score AS similarity
            FROM ranked_neighbors
            WHERE neighbor_rank <= :neighborLimit
            ORDER BY candidate_id ASC, neighbor_rank ASC
            """, nativeQuery = true)
    List<CandidateNeighborView> findRecommendationNeighbors(@Param("userId") Long userId,
                                                            @Param("historyLimit") int historyLimit,
                                                            @Param("candidateLimit") int candidateLimit,
                                                            @Param("neighborLimit") int neighborLimit);

    @Modifying
    @Query(value = """
            INSERT INTO event_similarities (event_a, event_b, score, updated_at)
            VALUES (:eventA, :eventB, :score, :updatedAt)
            ON CONFLICT (event_a, event_b) DO UPDATE
            SET score = EXCLUDED.score,
                updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    int upsert(@Param("eventA") Long eventA,
               @Param("eventB") Long eventB,
               @Param("score") double score,
               @Param("updatedAt") Instant updatedAt);

    List<EventSimilarity> findAllByIdEventAOrIdEventB(Long eventA, Long eventB);

    @Query(value = """
            SELECT CASE WHEN similarity.event_a = :eventId
                        THEN similarity.event_b ELSE similarity.event_a END AS "eventId",
                   similarity.score AS score
            FROM event_similarities similarity
            WHERE similarity.event_a = :eventId OR similarity.event_b = :eventId
            ORDER BY similarity.score DESC
            """, nativeQuery = true)
    List<SimilarEventView> findSimilarEvents(@Param("eventId") Long eventId, Pageable pageable);

    @Query(value = """
            SELECT CASE WHEN similarity.event_a = :eventId
                        THEN similarity.event_b ELSE similarity.event_a END AS "eventId",
                   similarity.score AS score
            FROM event_similarities similarity
            WHERE (similarity.event_a = :eventId OR similarity.event_b = :eventId)
              AND CASE WHEN similarity.event_a = :eventId
                       THEN similarity.event_b ELSE similarity.event_a END <> :eventId
              AND NOT EXISTS (
                  SELECT 1
                  FROM user_interactions interaction
                  WHERE interaction.user_id = :userId
                    AND interaction.event_id = CASE WHEN similarity.event_a = :eventId
                                                    THEN similarity.event_b ELSE similarity.event_a END
              )
            ORDER BY similarity.score DESC, "eventId" ASC
            """, nativeQuery = true)
    List<SimilarEventView> findUnseenSimilarEvents(@Param("eventId") Long eventId,
                                                   @Param("userId") Long userId,
                                                   Pageable pageable);

    @Query(value = """
            SELECT CASE WHEN similarity.event_a = :eventId
                        THEN similarity.event_b ELSE similarity.event_a END AS "eventId",
                   similarity.score AS score
            FROM event_similarities similarity
            JOIN user_interactions interaction
              ON interaction.user_id = :userId
             AND interaction.event_id = CASE WHEN similarity.event_a = :eventId
                                             THEN similarity.event_b ELSE similarity.event_a END
            WHERE similarity.event_a = :eventId OR similarity.event_b = :eventId
            ORDER BY similarity.score DESC
            """, nativeQuery = true)
    List<SimilarEventView> findNeighborsViewedByUser(@Param("eventId") Long eventId,
                                                     @Param("userId") Long userId,
                                                     Pageable pageable);
}
