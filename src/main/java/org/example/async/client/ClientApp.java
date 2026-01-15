package org.example.async.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.example.async.*;

import java.util.concurrent.CompletableFuture;

public class ClientApp {

    private final ManagedChannel channel;
    private final AsyncTaskServiceGrpc.AsyncTaskServiceBlockingStub blockingStub;
    private final AsyncTaskServiceGrpc.AsyncTaskServiceStub asyncStub;

    public ClientApp(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = AsyncTaskServiceGrpc.newBlockingStub(channel);
        this.asyncStub = AsyncTaskServiceGrpc.newStub(channel);
    }

    public void shutdown() {
        channel.shutdown();
    }
    public TaskHandle submitTask(String payload) {
        TaskRequest request = TaskRequest.newBuilder()
                .setTaskId(java.util.UUID.randomUUID().toString())
                .setPayload(payload)
                .setType(TaskType.CPU_INTENSIVE)
                .build();
        return blockingStub.submit(request);
    }

    public TaskResult awaitResult(TaskHandle handle) {
        CompletableFuture<TaskResult> future = new CompletableFuture<>();

        asyncStub.awaitResult(handle, new StreamObserver<>() {
            @Override
            public void onNext(TaskResult value) {
                if (value.getStatus() == TaskStatus.COMPLETED || value.getStatus() == TaskStatus.FAILED) {
                    future.complete(value);
                }
            }

            @Override
            public void onError(Throwable t) {
                future.completeExceptionally(new RuntimeException(t));
            }

            @Override
            public void onCompleted() { }
        });

        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException("Error awaiting task result", e);
        }
    }
    public static void main(String[] args) throws InterruptedException {
        ClientApp client = new ClientApp("localhost", 50051);

        TaskHandle handle = client.submitTask("Hello, SERVER!");
        System.out.println("Task submitted: " + handle.getTaskId());

        TaskResult result = client.awaitResult(handle);
        if (result != null) {
            System.out.println("Task result: " + result.getResult() + " | Status: " + result.getStatus());
        }

        client.shutdown();
    }
}
