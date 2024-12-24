package ru.alexpvl.grpcstorage.service;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.conditions.Conditions;
import io.tarantool.driver.api.space.TarantoolSpaceOperations;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import io.tarantool.driver.api.tuple.TarantoolTupleFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.alexpvl.grpcstorage.grpc.CountRequest;
import ru.alexpvl.grpcstorage.grpc.CountResponse;
import ru.alexpvl.grpcstorage.grpc.DeleteRequest;
import ru.alexpvl.grpcstorage.grpc.GetRequest;
import ru.alexpvl.grpcstorage.grpc.GrpcStorageServiceGrpc;
import ru.alexpvl.grpcstorage.grpc.KeyValue;
import ru.alexpvl.grpcstorage.grpc.PutRequest;
import ru.alexpvl.grpcstorage.grpc.RangeRequest;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrpcStorageService extends GrpcStorageServiceGrpc.GrpcStorageServiceImplBase {
    private final TarantoolSpaceOperations<TarantoolTuple, TarantoolResult<TarantoolTuple>> kvSpace;
    private final TarantoolTupleFactory tupleFactory;

    @Override
    public void put(PutRequest request, StreamObserver<Empty> responseObserver) {
        try {
            TarantoolTuple tuple = tupleFactory.create(request.getKey(), request.getValue().toByteArray());
            TarantoolResult<TarantoolTuple> putTuple = kvSpace.replace(tuple).get();

            if (!putTuple.isEmpty()) {
                log.info("Put tuple: {}", putTuple.get(0));
            } else {
                log.info("Put tuple: [empty result]");
            }

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in put method", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void get(GetRequest request, StreamObserver<KeyValue> responseObserver) {
        try {
            TarantoolResult<TarantoolTuple> result = kvSpace.select(equalsCondition(request.getKey())).get();

            if (result.isEmpty()) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription("Key not found: " + request.getKey())
                                .asRuntimeException()
                );
                return;
            }

            TarantoolTuple tuple = result.get(0);
            byte[] valueBytes = tuple.getByteArray("value");
            if (valueBytes == null) {
                valueBytes = new byte[0];
            }

            KeyValue keyValue = KeyValue.newBuilder()
                    .setKey(tuple.getString("key"))
                    .setValue(ByteString.copyFrom(valueBytes))
                    .build();

            responseObserver.onNext(keyValue);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in get method", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<Empty> responseObserver) {
        try {
            kvSpace.delete(equalsCondition(request.getKey())).get();

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in delete method", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void range(RangeRequest request, StreamObserver<KeyValue> responseObserver) {
        String keyFrom = request.getKeyFrom();
        String keyTo = request.getKeyTo();

        try {
            TarantoolResult<TarantoolTuple> result = kvSpace.select(equalsCondition(keyFrom)).get();

            for (TarantoolTuple tuple : result) {
                String currentKey = tuple.getString("key");
                if (currentKey == null) {
                    continue;
                }

                if (currentKey.compareTo(keyTo) > 0) {
                    break;
                }

                byte[] valueBytes = tuple.getByteArray("value");
                if (valueBytes == null) {
                    valueBytes = new byte[0];
                }

                KeyValue keyValue = KeyValue.newBuilder()
                        .setKey(currentKey)
                        .setValue(ByteString.copyFrom(valueBytes))
                        .build();

                responseObserver.onNext(keyValue);
            }

            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in range method", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void count(CountRequest request, StreamObserver<CountResponse> responseObserver) {
        try {
            TarantoolResult<TarantoolTuple> result = kvSpace.select(Conditions.any()).get();
            CountResponse countResponse = CountResponse.newBuilder()
                    .setCount(result.size())
                    .build();

            responseObserver.onNext(countResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in count method", e);
            responseObserver.onError(e);
        }
    }

    private Conditions equalsCondition(String key) {
        return Conditions.indexEquals("primary_idx", Collections.singletonList(key));
    }
}
