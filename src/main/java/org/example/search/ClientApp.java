package org.example.search;

import org.example.search.client.SearchClient;

import java.util.List;
import java.util.Random;

public class ClientApp {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 50051;

        SearchClient client = new SearchClient(host, port);

        try {
            for (int i = 1; i <= 10000; i++) {
                String id = String.valueOf(i);
                String title = "Document " + i;
                String content = "This is a test document number " + i;
                boolean added = client.addDocument(id, title, content);
                System.out.println("Document added: " + added + " (" + title + ")");
            }

            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            System.out.println("\n--- TEXT search ---");
            List<SearchResult> textResults = client.search("test", SearchMethod.TEXT);
            for (SearchResult r : textResults) {
                System.out.println("- " + r.getId() + ": " + r.getTitle());
            }
            System.out.println("\n--- VECTOR search ---");
            List<SearchResult> vectorResults = client.search("document number 3", SearchMethod.VECTOR);
            for (SearchResult r : vectorResults) {
                System.out.println("- " + r.getId() + ": " + r.getTitle());
            }
            System.out.println("\n--- VECTOR search benchmark ---");
            Random random = new Random();
            long start = System.currentTimeMillis();
            for (int i = 0; i < 50; i++) {
                String query = "document " + (random.nextInt(5) + 1);
                client.search(query, SearchMethod.VECTOR);
            }
            long end = System.currentTimeMillis();
            System.out.println("Performed 50 vector searches in " + (end - start) + " ms");

        } finally {
            client.shutdown();
        }
    }
}
