package org.example.async.model;

import java.util.concurrent.CompletableFuture;

public class TaskState {
    private final String id;
    private volatile Status status;
    private final CompletableFuture<String> result = new CompletableFuture<>();

    public enum Status {
        PENDING, RUNNING, COMPLETED, FAILED
    }

    public TaskState(String id) {
        this.id = id;
        this.status = Status.PENDING;
    }

    public String getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public CompletableFuture<String> getResultFuture() {
        return result;
    }
}
