import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.example.async.TaskHandle;
import org.example.async.client.ClientApp;
import org.example.async.TaskResult;
import org.example.async.TaskStatus;
import org.example.async.service.AsyncTaskServiceImpl;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsyncTaskClientIntegrationTest {

    private static Server server;
    private static int port;
    private ClientApp client;

    @BeforeAll
    static void startServer() throws Exception {
        port = 50052; // Можно любой свободный порт
        server = NettyServerBuilder.forPort(port)
                .addService(new AsyncTaskServiceImpl())
                .build()
                .start();
        System.out.println("Test gRPC server started on port " + port);
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @BeforeEach
    void setUp() {
        client = new ClientApp("localhost", port);
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.shutdown();
        }
    }

    @Test
    void submitTask_returnsNonNullTaskHandle() {
        TaskHandle handle = client.submitTask("Hello, Integration!");
        assertNotNull(handle);
        assertNotNull(handle.getTaskId());
        assertFalse(handle.getTaskId().isEmpty());
    }

    @Test
    void awaitResult_returnsCompletedTaskResult() throws InterruptedException {
        TaskHandle handle = client.submitTask("Hello, Integration!");
        TaskResult result = client.awaitResult(handle);

        assertNotNull(result);
        assertEquals(TaskStatus.COMPLETED, result.getStatus());
        assertEquals(handle.getTaskId(), result.getTaskId());
        assertNotNull(result.getResult());
    }
}
