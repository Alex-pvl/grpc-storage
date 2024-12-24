package ru.alexpvl.grpcstorage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("application.tarantool")
public record TarantoolProperties(
       Credentials credentials,
       Database database
) {
    public record Credentials(
            String username,
            String password
    ) {
    }

    public record Database(
            String host,
            int port,
            String spaceName
    ) {
    }
}
