package com.example.numbers.server;

import com.example.numbers.NumbersGeneratorGrpc;
import com.example.numbers.NumbersRequest;
import com.example.numbers.NumbersResponse;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class NumberServer {
    private Server server;

    private void start() throws IOException {
        int port = 50051;

        server = ServerBuilder.forPort(port)
                .addService(new NumbersGeneratorImpl())
                .build()
                .start();

        System.out.println("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** shutting down gRPC server since JVM is shutting down");

            try {
                NumberServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }

            System.err.println("*** server shut down");
        }));
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final NumberServer server = new NumberServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class NumbersGeneratorImpl extends NumbersGeneratorGrpc.NumbersGeneratorImplBase {
        @Override
        public void generateNumbers(NumbersRequest request, StreamObserver<NumbersResponse> responseObserver) {
            int firstValue = request.getFirstValue();
            int lastValue = request.getLastValue();

            try {
                for (int i = firstValue + 2; i <= lastValue; i++) {
                    NumbersResponse response = NumbersResponse.newBuilder().setValue(i).build();
                    responseObserver.onNext(response);

                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            responseObserver.onCompleted();
        }
    }
}
