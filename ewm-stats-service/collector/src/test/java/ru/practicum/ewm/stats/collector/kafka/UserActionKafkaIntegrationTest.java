package ru.practicum.ewm.stats.collector.kafka;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.serialization.AvroDeserializer;
import ru.practicum.ewm.stats.avro.serialization.AvroSerializer;
import ru.practicum.ewm.stats.collector.mapper.UserActionMapper;
import ru.practicum.ewm.stats.collector.service.UserActionGrpcService;
import ru.practicum.stats.service.collector.ActionTypeProto;
import ru.practicum.stats.service.collector.UserActionProto;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserActionKafkaIntegrationTest {

    private static final String TOPIC = "stats.user-actions.v1";

    @Test
    void shouldCollectViewOverGrpcServiceAndPublishEveryFieldToKafka() throws Exception {
        EmbeddedKafkaBroker broker = new EmbeddedKafkaKraftBroker(1, 1, TOPIC);
        broker.afterPropertiesSet();
        try {
            Map<String, Object> producerProperties = KafkaTestUtils.producerProps(broker);
            KafkaTemplate<Long, UserActionAvro> template = new KafkaTemplate<>(
                    new DefaultKafkaProducerFactory<>(producerProperties,
                            new LongSerializer(), new AvroSerializer<>()));

            Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps("collector-test", "true", broker);
            Consumer<Long, UserActionAvro> consumer = new DefaultKafkaConsumerFactory<>(consumerProperties,
                    new LongDeserializer(), new AvroDeserializer<>(UserActionAvro.class)).createConsumer();
            broker.consumeFromAnEmbeddedTopic(consumer, TOPIC);

            Instant timestamp = Instant.parse("2026-07-12T12:34:56.789Z");
            UserActionProto request = UserActionProto.newBuilder()
                    .setUserId(7L).setEventId(8L)
                    .setActionType(ActionTypeProto.ACTION_VIEW)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(timestamp.getEpochSecond()).setNanos(timestamp.getNano()))
                    .build();
            UserActionGrpcService service = new UserActionGrpcService(new UserActionMapper(), template);
            ReflectionTestUtils.setField(service, "userActionsTopic", TOPIC);
            RecordingObserver observer = new RecordingObserver();

            service.collectUserAction(request, observer);

            ConsumerRecord<Long, UserActionAvro> record = KafkaTestUtils.getSingleRecord(
                    consumer, TOPIC, Duration.ofSeconds(10));
            assertEquals(7L, record.key());
            assertEquals(7L, record.value().getUserId());
            assertEquals(8L, record.value().getEventId());
            assertEquals(ActionTypeAvro.VIEW, record.value().getActionType());
            assertEquals(timestamp, record.value().getTimestamp());
            assertEquals(1, observer.values.size());
            assertTrue(observer.completed);

            consumer.close();
            template.destroy();
        } finally {
            broker.destroy();
        }
    }

    private static class RecordingObserver implements StreamObserver<Empty> {
        private final List<Empty> values = new ArrayList<>();
        private boolean completed;

        @Override
        public void onNext(Empty value) {
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
