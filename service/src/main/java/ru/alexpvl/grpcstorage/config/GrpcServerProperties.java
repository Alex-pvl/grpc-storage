package ru.alexpvl.grpcstorage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("grpc.server")
public record GrpcServerProperties(Integer port) {
    private static final Integer GRPC_DEFAULT_PORT = 9091;

    public GrpcServerProperties(Integer port) {
        this.port = port == null ? GRPC_DEFAULT_PORT : port;
    }
}

