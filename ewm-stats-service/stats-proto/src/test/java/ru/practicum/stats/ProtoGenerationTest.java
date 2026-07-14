package ru.practicum.stats;

import com.google.protobuf.Descriptors;
import io.grpc.MethodDescriptor;
import org.junit.jupiter.api.Test;
import ru.practicum.stats.service.collector.ActionTypeProto;
import ru.practicum.stats.service.collector.UserActionControllerGrpc;
import ru.practicum.stats.service.collector.UserActionProto;
import ru.practicum.stats.service.dashboard.InteractionsCountRequestProto;
import ru.practicum.stats.service.dashboard.RecommendationsControllerGrpc;
import ru.practicum.stats.service.dashboard.RecommendedEventProto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtoGenerationTest {

    @Test
    void shouldGenerateCollectorContract() {
        assertEquals(0, ActionTypeProto.ACTION_VIEW.getNumber());
        assertEquals(1, ActionTypeProto.ACTION_REGISTER.getNumber());
        assertEquals(2, ActionTypeProto.ACTION_LIKE.getNumber());
        assertEquals(1, UserActionProto.getDescriptor().findFieldByName("user_id").getNumber());
        assertEquals(4, UserActionProto.getDescriptor().findFieldByName("timestamp").getNumber());
        assertEquals(MethodDescriptor.MethodType.UNARY,
                UserActionControllerGrpc.getCollectUserActionMethod().getType());
    }

    @Test
    void shouldGenerateAnalyzerContract() {
        Descriptors.FieldDescriptor eventIds = InteractionsCountRequestProto.getDescriptor()
                .findFieldByName("event_ids");

        assertEquals(1, eventIds.getNumber());
        assertTrue(eventIds.isRepeated());
        assertEquals(1, RecommendedEventProto.getDescriptor().findFieldByName("event_id").getNumber());
        assertEquals(2, RecommendedEventProto.getDescriptor().findFieldByName("score").getNumber());
        assertEquals(MethodDescriptor.MethodType.SERVER_STREAMING,
                RecommendationsControllerGrpc.getGetRecommendationsForUserMethod().getType());
        assertEquals(MethodDescriptor.MethodType.SERVER_STREAMING,
                RecommendationsControllerGrpc.getGetSimilarEventsMethod().getType());
        assertEquals(MethodDescriptor.MethodType.SERVER_STREAMING,
                RecommendationsControllerGrpc.getGetInteractionsCountMethod().getType());
    }
}
