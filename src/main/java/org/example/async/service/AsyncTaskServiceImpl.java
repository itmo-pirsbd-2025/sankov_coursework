package org.example.async.service;

import io.grpc.stub.StreamObserver;
import org.example.async.*;
import org.example.async.executor.TaskExecutor;
import org.example.async.executor.TaskRegistry;
import org.example.async.model.TaskState;

import java.util.UUID;

public class AsyncTaskServiceImpl extends AsyncTaskServiceGrpc.AsyncTaskServiceImplBase {

    private final TaskRegistry registry = new TaskRegistry();
    private final TaskExecutor executor = new TaskExecutor();

    @Override
    public void submit(TaskRequest request, StreamObserver<TaskHandle> responseObserver) {
        String id = UUID.randomUUID().toString();

        TaskState task = registry.create(id);
        executor.submit(task, request.getPayload());

        responseObserver.onNext(TaskHandle.newBuilder()
                .setTaskId(id)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void awaitResult(TaskHandle request, StreamObserver<TaskResult> responseObserver) {
        TaskState task = registry.get(request.getTaskId());

        if (task == null) {
            responseObserver.onError(
                    new IllegalArgumentException("Unknown task: " + request.getTaskId()));
            return;
        }

        // начальное состояние
        responseObserver.onNext(toProto(task, null));

        task.getResultFuture().whenComplete((res, err) -> {
            if (err != null) {
                responseObserver.onNext(TaskResult.newBuilder()
                        .setTaskId(task.getId())
                        .setStatus(TaskStatus.FAILED)
                        .setError(err.getMessage())
                        .build());
            } else {
                responseObserver.onNext(TaskResult.newBuilder()
                        .setTaskId(task.getId())
                        .setStatus(TaskStatus.COMPLETED)
                        .setResult(res)
                        .build());
            }
            responseObserver.onCompleted();
        });
    }

    private TaskResult toProto(TaskState task, String result) {
        return TaskResult.newBuilder()
                .setTaskId(task.getId())
                .setStatus(TaskStatus.valueOf(task.getStatus().name()))
                .build();
    }
}
