package ru.practicum.ewm.stats.analyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "user_interactions")
@NoArgsConstructor
@AllArgsConstructor
public class UserInteraction {

    @EmbeddedId
    private UserInteractionId id;

    @Column(nullable = false)
    private Double weight;

    @Column(name = "last_action_timestamp", nullable = false)
    private Instant lastActionTimestamp;
}
