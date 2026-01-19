import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.example.search.*;
import org.example.search.client.SearchClient;
import org.example.search.service.SearchServiceImpl;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchServiceTest {

    private SearchServiceImpl service;
    private SearchClient client;
    private Server server;

    @BeforeAll
    void setupServer() throws IOException, InterruptedException {
        service = new SearchServiceImpl();

        server = NettyServerBuilder.forPort(50052)
                .addService(service)
                .build()
                .start();

        client = new SearchClient("localhost", 50052);
        for (int i = 1; i <= 10_000; i++) {
            String id = String.valueOf(i);
            String title = "Document " + i;
            String content = "This is a test document number " + i;
            boolean added = client.addDocument(id, title, content);
            assertTrue(added, "Document should be added successfully: " + id);
        }
        Thread.sleep(1_000);
    }

    @AfterAll
    void tearDown() throws InterruptedException {
        if (client != null) client.shutdown();
        if (server != null) server.shutdownNow();
    }

    @Test
    void addDocument_returnsTrue() {
        boolean added = client.addDocument("10001", "Title 10001", "Content 10001");
        assertTrue(added, "Document should be added successfully");
    }

    @Test
    void textSearch_likeClientApp() {
        List<SearchResult> results = client.search("test", SearchMethod.TEXT);
        assertNotNull(results, "Search results should not be null");
        assertFalse(results.isEmpty(), "Search results should not be empty");
    }

    @Test
    void vectorSearch_likeClientApp() {
        List<SearchResult> results = client.search("document number 3", SearchMethod.VECTOR);
        assertNotNull(results, "Vector search results should not be null");
        assertFalse(results.isEmpty(), "Vector search results should not be empty");
    }

    @Test
    void search_withEmptyQuery_returnsNonNull() {
        List<SearchResult> resultsText = client.search("", SearchMethod.TEXT);
        assertNotNull(resultsText, "Empty text query should return non-null list");

        List<SearchResult> resultsVector = client.search("", SearchMethod.VECTOR);
        assertNotNull(resultsVector, "Empty vector query should return non-null list");
    }

    @Test
    void search_withUnknownQuery_returnsNonNull() {
        List<SearchResult> resultsText = client.search("nonexistentword123", SearchMethod.TEXT);
        assertNotNull(resultsText, "Unknown text query should return non-null list");

        List<SearchResult> resultsVector = client.search("completely unrelated query", SearchMethod.VECTOR);
        assertNotNull(resultsVector, "Unknown vector query should return non-null list");
    }

    @Test
    void vectorSearch_benchmark_likeClientApp() {
        Random random = new Random();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 50; i++) {
            String query = "document " + (random.nextInt(5) + 1);
            List<SearchResult> results = client.search(query, SearchMethod.VECTOR);
            assertNotNull(results, "Benchmark search results should not be null");
        }
        long end = System.currentTimeMillis();
        System.out.println("Performed 50 vector searches in " + (end - start) + " ms");
    }
}
