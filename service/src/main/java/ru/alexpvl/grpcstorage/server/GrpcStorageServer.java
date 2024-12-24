package ru.alexpvl.grpcstorage.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import ru.alexpvl.grpcstorage.config.GrpcServerProperties;
import ru.alexpvl.grpcstorage.service.GrpcStorageService;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(GrpcServerProperties.class)
public class GrpcStorageServer implements SmartLifecycle {
    private static final int TERMINATION_TIMEOUT = 10;
    private final GrpcServerProperties grpcServerProperties;
    private final GrpcStorageService grpcStorageService;

    private Server server = null;
    private boolean running = false;

    @Override
    public void start() {
        this.server = ServerBuilder.forPort(grpcServerProperties.port())
                .addService(grpcStorageService)
                .addService(ProtoReflectionService.newInstance())
                .build();

        log.info("gRPC server is starting...");
        try {
            server.start();
            this.running = true;
            log.info("gRPC server started on port {}", grpcServerProperties.port());
        } catch (Exception e) {
            this.running = false;
            log.error("gRPC server isn't started on port {}", grpcServerProperties.port(), e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void stop() {
        try {
            log.info("gRPC server is shutting down...");
            server.shutdown();
            server.awaitTermination(TERMINATION_TIMEOUT, TimeUnit.SECONDS);
            log.info("gRPC server stopped.");
        } catch (InterruptedException e) {
            log.error("Error while shutting down gRPC server: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }
}
