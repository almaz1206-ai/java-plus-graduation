package ru.practicum.ewm.stats.analyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler analyzerKafkaErrorHandler(
            @Value("${analyzer.kafka.retry.interval-ms}") long interval,
            @Value("${analyzer.kafka.retry.max-attempts}") long maxAttempts) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new FixedBackOff(interval, Math.max(0, maxAttempts - 1)));
        errorHandler.setAckAfterHandle(false);
        return errorHandler;
    }
}
