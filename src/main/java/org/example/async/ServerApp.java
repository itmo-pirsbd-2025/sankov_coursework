package org.example.async;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.example.async.service.AsyncTaskServiceImpl;

import java.io.IOException;

public class ServerApp {

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 50051;

        Server server = NettyServerBuilder
                .forPort(port)
                .addService(new AsyncTaskServiceImpl())
                .build();

        server.start();
        System.out.println("gRPC server started on port " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down gRPC server...");
            server.shutdown();
        }));

        server.awaitTermination();
    }
}
