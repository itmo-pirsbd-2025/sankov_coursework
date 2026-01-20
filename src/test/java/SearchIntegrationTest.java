import org.example.search.client.SearchClient;
import org.example.search.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SearchIntegrationTest {

    @Test
    void testSearch() throws Exception {
        SearchClient client = new SearchClient("localhost", 50051);

        try {
            client.addDocument("doc1", "Java Lucene", "lucene search engine test");
            client.addDocument("doc2", "gRPC Server", "grpc java service");
            Thread.sleep(4000);
            List<SearchResult> textRes = client.search("lucene", SearchMethod.TEXT);
            System.out.println("TEXT results: " + textRes.size());
            assertFalse(textRes.isEmpty());
            List<SearchResult> vecRes = client.search("search engine", SearchMethod.VECTOR);
            System.out.println("VECTOR results: " + vecRes.size());
            assertNotNull(vecRes);

        } finally {
            client.shutdown();
        }
    }
}
