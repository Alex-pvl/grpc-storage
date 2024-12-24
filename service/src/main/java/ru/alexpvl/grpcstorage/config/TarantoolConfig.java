package ru.alexpvl.grpcstorage.config;

import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientFactory;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.TarantoolServerAddress;
import io.tarantool.driver.api.space.TarantoolSpaceOperations;
import io.tarantool.driver.api.tuple.DefaultTarantoolTupleFactory;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import io.tarantool.driver.api.tuple.TarantoolTupleFactory;
import io.tarantool.driver.mappers.factories.DefaultMessagePackMapperFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TarantoolProperties.class)
public class TarantoolConfig {

    @Bean
    public TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolClient(
            TarantoolProperties properties
    ) {
        return TarantoolClientFactory.createClient()
                .withAddresses(new TarantoolServerAddress(properties.database().host(), properties.database().port()))
                .withCredentials(properties.credentials().username(), properties.credentials().password())
                .build();
    }

    @Bean
    public TarantoolSpaceOperations<TarantoolTuple, TarantoolResult<TarantoolTuple>> kvSpace(
            TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolClient,
            TarantoolProperties properties
    ) {
        return tarantoolClient.space(properties.database().spaceName());
    }

    @Bean
    public TarantoolTupleFactory tupleFactory() {
        return new DefaultTarantoolTupleFactory(
                DefaultMessagePackMapperFactory.getInstance().defaultComplexTypesMapper()
        );
    }
}
