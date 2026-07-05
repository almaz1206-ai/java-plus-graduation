package ru.practicum.ewm.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.practicum.client.StatsClient;

import java.time.Duration;

@Configuration
public class StatsClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @Value("${stats.server.url}")
    private String statsServerUrl;

    @Bean
    @LoadBalanced
    public RestTemplate statsRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setReadTimeout(READ_TIMEOUT)
                .build();
    }

    @Bean
    public StatsClient statsClient(RestTemplate statsRestTemplate) {
        return new StatsClient(statsServerUrl, statsRestTemplate);
    }
}
