import io.grpc.stub.StreamObserver;
import org.example.async.*;
import org.example.async.client.ClientApp;
import org.example.async.service.AsyncTaskServiceImpl;
import org.junit.jupiter.api.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AsyncTaskServiceTest {

    private AsyncTaskServiceImpl service;
    private ClientApp client;

    @BeforeEach
    void setUp() {
        service = new AsyncTaskServiceImpl();
        client = new ClientApp("localhost", 50051);
    }

    @AfterEach
    void tearDown() {
        client.shutdown();
    }

    @Test
    void submit_returnsTaskHandle() {
        TaskRequest request = TaskRequest.newBuilder()
                .setPayload("Hello")
                .setTaskId(UUID.randomUUID().toString())
                .build();

        AtomicReference<TaskHandle> ref = new AtomicReference<>();
        service.submit(request, new StreamObserver<>() {
            @Override
            public void onNext(TaskHandle value) {
                ref.set(value);
            }

            @Override
            public void onError(Throwable t) {
                fail("submit should not fail: " + t.getMessage());
            }

            @Override
            public void onCompleted() { }
        });

        assertNotNull(ref.get(), "TaskHandle should not be null");
        assertNotNull(ref.get().getTaskId(), "Task ID should not be null");
    }

    @Test
    void awaitResult_returnsResult() throws Exception {
        TaskRequest request = TaskRequest.newBuilder()
                .setPayload("Test")
                .setTaskId(UUID.randomUUID().toString())
                .build();

        AtomicReference<TaskHandle> handleRef = new AtomicReference<>();
        service.submit(request, new StreamObserver<>() {
            @Override
            public void onNext(TaskHandle value) {
                handleRef.set(value);
            }

            @Override
            public void onError(Throwable t) {
                fail(t);
            }

            @Override
            public void onCompleted() { }
        });

        CompletableFuture<TaskResult> futureResult = new CompletableFuture<>();
        service.awaitResult(handleRef.get(), new StreamObserver<>() {
            @Override
            public void onNext(TaskResult value) {
                if (value.getStatus() == TaskStatus.COMPLETED || value.getStatus() == TaskStatus.FAILED) {
                    futureResult.complete(value);
                }
            }

            @Override
            public void onError(Throwable t) {
                futureResult.completeExceptionally(t);
            }

            @Override
            public void onCompleted() { }
        });

        TaskResult result = futureResult.get();
        assertEquals(TaskStatus.COMPLETED, result.getStatus());
        assertEquals("Processed: TEST", result.getResult());
    }

    @Test
    void awaitResult_forUnknownTask_returnsError() throws Exception {
        TaskHandle fakeHandle = TaskHandle.newBuilder().setTaskId("fake-id").build();

        CompletableFuture<Throwable> futureError = new CompletableFuture<>();
        service.awaitResult(fakeHandle, new StreamObserver<>() {
            @Override
            public void onNext(TaskResult value) { }

            @Override
            public void onError(Throwable t) {
                futureError.complete(t);
            }

            @Override
            public void onCompleted() { }
        });

        Throwable error = futureError.get();
        assertTrue(error instanceof IllegalArgumentException);
        assertTrue(error.getMessage().contains("Unknown task"));
    }

    // === Клиентские тесты ===
    @Test
    void client_submitTask_returnsNonNullTaskHandle() {
        TaskHandle handle = client.submitTask("Hello, Test!");
        assertNotNull(handle);
        assertNotNull(handle.getTaskId());
        assertFalse(handle.getTaskId().isEmpty());
    }

    @Test
    void client_awaitResult_returnsCompletedTaskResult() throws InterruptedException {
        TaskHandle handle = client.submitTask("Hello, Test!");
        TaskResult result = client.awaitResult(handle);

        assertNotNull(result);
        assertEquals(handle.getTaskId(), result.getTaskId());
        assertEquals(TaskStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getResult());
    }

    @Test
    void client_awaitResult_withUnknownTask_throwsException() {
        TaskHandle fakeHandle = TaskHandle.newBuilder().setTaskId("unknown-id").build();
        assertThrows(RuntimeException.class, () -> client.awaitResult(fakeHandle));
    }


}
