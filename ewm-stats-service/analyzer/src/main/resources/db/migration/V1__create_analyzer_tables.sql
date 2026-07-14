CREATE TABLE user_interactions (
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    last_action_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_user_interactions PRIMARY KEY (user_id, event_id),
    CONSTRAINT chk_user_interactions_weight_non_negative CHECK (weight >= 0.0)
);

CREATE INDEX idx_user_interactions_user_id
    ON user_interactions (user_id);
CREATE INDEX idx_user_interactions_event_id
    ON user_interactions (event_id);
CREATE INDEX idx_user_interactions_user_timestamp
    ON user_interactions (user_id, last_action_timestamp DESC);

CREATE TABLE event_similarities (
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_event_similarities PRIMARY KEY (event_a, event_b),
    CONSTRAINT chk_event_similarities_normalized CHECK (event_a < event_b),
    CONSTRAINT chk_event_similarities_score CHECK (score >= 0.0 AND score <= 1.0)
);

CREATE INDEX idx_event_similarities_event_a
    ON event_similarities (event_a);
CREATE INDEX idx_event_similarities_event_b
    ON event_similarities (event_b);
