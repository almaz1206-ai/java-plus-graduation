package ru.practicum.ewm.stats.analyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "analyzer.recommendations")
public record RecommendationProperties(int historyLimit, int candidateLimit, int neighborLimit) {

    public RecommendationProperties {
        if (historyLimit <= 0 || candidateLimit <= 0 || neighborLimit <= 0) {
            throw new IllegalArgumentException("Recommendation limits must be positive");
        }
    }
}
