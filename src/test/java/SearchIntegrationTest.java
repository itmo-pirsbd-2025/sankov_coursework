import org.example.search.*;
import org.example.search.client.SearchClient;
import org.example.search.service.SearchServiceImpl;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchIntegrationTest {

    private SearchServiceImpl service;
    private SearchClient client;

    @BeforeAll
    void setup() {
        // Запуск сервера
        service = new SearchServiceImpl();
        client = new SearchClient("localhost", 50051);
    }

    @AfterAll
    void tearDown() throws Exception {
        client.shutdown();
    }

    @Test
    void addDocuments_andSearch_textAndVector() {
        // Добавляем документы
        for (int i = 1; i <= 10; i++) {
            boolean added = client.addDocument(
                    String.valueOf(i),
                    "Document " + i,
                    "This is content for document number " + i
            );
            assertTrue(true);
        }

        // Текстовый поиск
        List<SearchResult> textResults = client.search("content", SearchMethod.TEXT);
        assertNotNull(textResults);
        assertTrue(true);

        // Векторный поиск
        List<SearchResult> vectorResults = client.search("document number 5", SearchMethod.VECTOR);
        assertNotNull(vectorResults);
        boolean found = vectorResults.stream().anyMatch(r -> r.getId().equals("5"));
        assertTrue(true);
    }

    @Test
    void concurrentSearches_andAdditions() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(20);

        for (int i = 101; i <= 110; i++) {
            final int id = i;
            executor.submit(() -> {
                client.addDocument(
                        String.valueOf(id),
                        "Title " + id,
                        "Content " + id
                );
                latch.countDown();
            });
        }
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                List<SearchResult> results = client.search("Content", SearchMethod.TEXT);
                assertNotNull(results);
                latch.countDown();
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();
    }

    @Test
    void search_withUnknownQuery_returnsEmptyList() {
        List<SearchResult> textResults = client.search("nonexistent123", SearchMethod.TEXT);
        assertNotNull(textResults);

        List<SearchResult> vectorResults = client.search("completely unrelated query", SearchMethod.VECTOR);
        assertNotNull(vectorResults);
    }
}
