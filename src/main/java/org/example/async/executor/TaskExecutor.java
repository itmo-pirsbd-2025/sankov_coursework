package org.example.async.executor;

import org.example.async.model.TaskState;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskExecutor {

    private final ExecutorService pool =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public void submit(TaskState task, String payload) {
        pool.submit(() -> {
            try {
                task.setStatus(TaskState.Status.RUNNING);

                // имитация тяжёлой работы
                Thread.sleep(500 + payload.length() * 10L);

                String result = "Processed: " + payload.toUpperCase();

                task.setStatus(TaskState.Status.COMPLETED);
                task.getResultFuture().complete(result);

            } catch (Exception e) {
                task.setStatus(TaskState.Status.FAILED);
                task.getResultFuture().completeExceptionally(e);
            }
        });
    }
}
