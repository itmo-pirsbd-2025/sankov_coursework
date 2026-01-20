package org.example.search;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.Collections;

public class SearchStressTest {

    static final String HOST = "localhost";
    static final int PORT = 50051;

    public static void main(String[] args) throws Exception {
        ManagedChannel ch = ManagedChannelBuilder
                .forAddress(HOST, PORT)
                .usePlaintext()
                .build();

        SearchServiceGrpc.SearchServiceBlockingStub stub =
                SearchServiceGrpc.newBlockingStub(ch);
        System.out.println("loading docs...");

        for (int i = 0; i < 1000; i++) {
            AddDocumentRequest req = AddDocumentRequest.newBuilder()
                    .setId("doc" + i)
                    .setTitle("Test doc " + i)
                    .setContent("test content for document number " + i)
                    .build();
            stub.addDocument(req);
        }
        Thread.sleep(5000);

        System.out.println("\n=== LOAD TEST 30 сек ===");
        long startTime = System.currentTimeMillis();
        ArrayList<Long> times = new ArrayList<>();  // все времена
        int total = 0;
        int errors = 0;

        while ((System.currentTimeMillis() - startTime) < 30000) {
            long start = System.nanoTime();
            try {
                if (Math.random() < 0.7) {
                    SearchRequest req = SearchRequest.newBuilder()
                            .setQuery("test")
                            .setMethod(SearchMethod.TEXT)
                            .build();
                    stub.search(req);
                } else {
                    SearchRequest req = SearchRequest.newBuilder()
                            .setQuery("document")
                            .setMethod(SearchMethod.VECTOR)
                            .build();
                    stub.search(req);
                }
                long time = System.nanoTime() - start;
                times.add(time);
                total++;
            } catch (Exception e) {
                errors++;
            }
        }

        ch.shutdown();

        System.out.println("\n=== RESULTS ===");
        System.out.println("total requests: " + total);
        System.out.println("errors: " + errors);
        System.out.println("rps: " + (total * 1000 / 30));

        if (!times.isEmpty()) {
            Collections.sort(times);

            long p50 = getPercentile(times, 50);
            long p90 = getPercentile(times, 90);
            long p95 = getPercentile(times, 95);
            long p99 = getPercentile(times, 99);

            System.out.println("\n=== LATENCY (микросекунды) ===");
            System.out.println("p50: " + (p50 / 1000));
            System.out.println("p90: " + (p90 / 1000));
            System.out.println("p95: " + (p95 / 1000));
            System.out.println("p99: " + (p99 / 1000));
        }
    }
    static long getPercentile(ArrayList<Long> sortedTimes, int percent) {
        int idx = (int) (percent / 100.0 * sortedTimes.size());
        if (idx >= sortedTimes.size()) idx = sortedTimes.size() - 1;
        return sortedTimes.get(idx);
    }
}
