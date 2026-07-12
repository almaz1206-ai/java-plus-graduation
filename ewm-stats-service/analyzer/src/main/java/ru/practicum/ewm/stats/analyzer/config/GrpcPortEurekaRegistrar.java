package ru.practicum.ewm.stats.analyzer.config;

import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GrpcPortEurekaRegistrar {

    private static final String GRPC_PORT_METADATA_KEY = "gRPC_port";

    private final EurekaRegistration eurekaRegistration;

    @EventListener
    public void registerActualPort(GrpcServerStartedEvent event) {
        String port = Integer.toString(event.getPort());
        eurekaRegistration.getInstanceConfig().getMetadataMap().put(GRPC_PORT_METADATA_KEY, port);
        eurekaRegistration.getApplicationInfoManager().getInfo().getMetadata().put(GRPC_PORT_METADATA_KEY, port);
        eurekaRegistration.getApplicationInfoManager().getInfo().setIsDirty();
    }
}
