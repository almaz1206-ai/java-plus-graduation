package ru.practicum.ewm.stats.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({CollectorClient.class, RecommendationsClient.class})
public class StatsClientAutoConfiguration {
}
