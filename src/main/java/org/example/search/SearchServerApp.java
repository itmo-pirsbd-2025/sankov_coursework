package org.example.search;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.example.search.service.SearchServiceImpl;

public class SearchServerApp {

    public static void main(String[] args) throws Exception {
        int port = 50051;

        Server server = NettyServerBuilder.forPort(port)
                .addService(new SearchServiceImpl())
                .build()
                .start();

        System.out.println("Search gRPC server started on port " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down search server...");
            server.shutdown();
        }));

        server.awaitTermination();
    }
}
