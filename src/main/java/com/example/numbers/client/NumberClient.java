package com.example.numbers.client;

import com.example.numbers.NumbersGeneratorGrpc;
import com.example.numbers.NumbersRequest;
import com.example.numbers.NumbersResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NumberClient {
    private final ManagedChannel channel;
    private final NumbersGeneratorGrpc.NumbersGeneratorStub asyncStub;

    public NumberClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }

    private NumberClient(ManagedChannel channel) {
        this.channel = channel;
        asyncStub = NumbersGeneratorGrpc.newStub(channel);
    }

    public void getNumbers(int firstValue, int lastValue) throws InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<NumbersResponse> responseObserver = new StreamObserver<NumbersResponse>() {
            AtomicInteger currentValue = new AtomicInteger(0);
            int serverValue = 0;

            @Override
            public void onNext(NumbersResponse response) {
                try {
                    if (serverValue != response.getValue()) {
                        System.out.println("currentValue:" + currentValue.addAndGet(1));
                    } else {
                        currentValue.addAndGet(serverValue + 1);
                        System.out.println("currentValue:" + currentValue);
                    }
                    serverValue = response.getValue();
                    System.out.println("new value:" + serverValue);
                    Thread.sleep(1000);
                    System.out.println("currentValue:" + currentValue.addAndGet(serverValue + 1));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("request completed");
                finishLatch.countDown();
            }
        };

        asyncStub.generateNumbers(NumbersRequest.newBuilder()
                .setFirstValue(firstValue)
                .setLastValue(lastValue)
                .build(), responseObserver);

        finishLatch.await(1, TimeUnit.MINUTES);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        NumberClient client = new NumberClient("localhost", 50051);
        System.out.println("numbers Client is starting...");
        client.getNumbers(0, 30);
        client.shutdown();
    }
}
