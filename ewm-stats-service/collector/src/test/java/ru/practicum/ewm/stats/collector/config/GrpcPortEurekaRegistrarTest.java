package ru.practicum.ewm.stats.collector.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.netflix.eureka.CloudEurekaInstanceConfig;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrpcPortEurekaRegistrarTest {

    @Test
    void shouldReplaceConfiguredZeroWithActualGrpcPort() {
        EurekaRegistration registration = mock(EurekaRegistration.class);
        CloudEurekaInstanceConfig instanceConfig = mock(CloudEurekaInstanceConfig.class);
        ApplicationInfoManager infoManager = mock(ApplicationInfoManager.class);
        InstanceInfo instanceInfo = mock(InstanceInfo.class);
        GrpcServerStartedEvent event = mock(GrpcServerStartedEvent.class);
        Map<String, String> metadata = new HashMap<>();
        Map<String, String> publishedMetadata = new HashMap<>();
        metadata.put(GrpcPortEurekaRegistrar.GRPC_PORT_METADATA_KEY, "0");

        when(registration.getInstanceConfig()).thenReturn(instanceConfig);
        when(instanceConfig.getMetadataMap()).thenReturn(metadata);
        when(registration.getApplicationInfoManager()).thenReturn(infoManager);
        when(infoManager.getInfo()).thenReturn(instanceInfo);
        when(instanceInfo.getMetadata()).thenReturn(publishedMetadata);
        when(event.getPort()).thenReturn(49152);

        new GrpcPortEurekaRegistrar(registration).registerActualPort(event);

        assertEquals("49152", metadata.get(GrpcPortEurekaRegistrar.GRPC_PORT_METADATA_KEY));
        assertEquals("49152", publishedMetadata.get(GrpcPortEurekaRegistrar.GRPC_PORT_METADATA_KEY));
        verify(instanceInfo).setIsDirty();
    }
}
