package ru.practicum.ewm.stats.analyzer.config;

import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.serialization.EventSimilarityAvroDeserializer;
import ru.practicum.ewm.stats.avro.serialization.UserActionAvroDeserializer;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, UserActionAvro>
            userActionKafkaListenerContainerFactory(KafkaProperties properties,
                                                    DefaultErrorHandler analyzerKafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<Long, UserActionAvro> factory = newFactory();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(
                properties.buildConsumerProperties(null),
                new LongDeserializer(), new UserActionAvroDeserializer()));
        factory.setCommonErrorHandler(analyzerKafkaErrorHandler);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventSimilarityAvro>
            eventSimilarityKafkaListenerContainerFactory(KafkaProperties properties,
                                                         DefaultErrorHandler analyzerKafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, EventSimilarityAvro> factory = newFactory();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(
                properties.buildConsumerProperties(null),
                new StringDeserializer(), new EventSimilarityAvroDeserializer()));
        factory.setCommonErrorHandler(analyzerKafkaErrorHandler);
        return factory;
    }

    private <K, T> ConcurrentKafkaListenerContainerFactory<K, T> newFactory() {
        ConcurrentKafkaListenerContainerFactory<K, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
