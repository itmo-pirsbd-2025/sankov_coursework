import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.example.search.*;
import org.example.search.client.SearchClient;
import org.example.search.service.SearchServiceImpl;
import org.junit.jupiter.api.*;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import org.example.search.*;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchServiceTest {

    SearchServiceImpl service;
    SearchClient client;
    Server server;

    @BeforeAll
    void startServer() throws Exception {
        service = new SearchServiceImpl();

        server = NettyServerBuilder.forPort(50053)
                .addService(service)
                .build().start();

        // ждем запуска
        Thread.sleep(1000);

        client = new SearchClient("localhost", 50053);

        // добавляем доки для тестов (не 10к!)
        for (int i = 1; i <= 20; i++) {
            client.addDocument("" + i, "Doc " + i, "test document " + i);
        }
        Thread.sleep(2000);
    }

    @AfterAll
    void cleanup() throws InterruptedException {
        if (client != null) {
            client.shutdown();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void addDoc() {
        boolean ok = client.addDocument("999", "new doc", "content");
        assertTrue(ok);
    }

    @Test
    void textSearch() {
        var res = client.search("test", SearchMethod.TEXT);
        assertNotNull(res);
        assertFalse(res.isEmpty());
    }

    @Test
    void vectorSearch() {
        var res = client.search("document 5", SearchMethod.VECTOR);
        assertNotNull(res);
        assertFalse(res.isEmpty());
    }

    @Test
    void emptyQuery() {
        assertNotNull(client.search("", SearchMethod.TEXT));
        assertNotNull(client.search("", SearchMethod.VECTOR));
    }

    @Test
    void badQuery() {
        assertNotNull(client.search("xyz123", SearchMethod.TEXT));
        assertNotNull(client.search("total nonsense", SearchMethod.VECTOR));
    }

    @Test
    void speedTest() {
        long t0 = System.currentTimeMillis();
        Random r = new Random();

        for (int i = 0; i < 30; i++) {
            String q = "document " + (r.nextInt(10) + 1);
            var res = client.search(q, SearchMethod.VECTOR);
            assertNotNull(res);
        }

        System.out.println("30 searches: " + (System.currentTimeMillis() - t0) + "ms");
    }
}
