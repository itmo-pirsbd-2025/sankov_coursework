package org.example.async.executor;

import org.example.async.model.TaskState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskRegistry {

    private final Map<String, TaskState> tasks = new ConcurrentHashMap<>();

    public TaskState create(String id) {
        TaskState state = new TaskState(id);
        tasks.put(id, state);
        return state;
    }

    public TaskState get(String id) {
        return tasks.get(id);
    }
}
