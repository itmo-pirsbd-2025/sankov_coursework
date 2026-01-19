package org.example.search;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class SearchStressTest {

    private static final String HOST = "localhost";
    private static final int PORT = 50051;

    private static final int CLIENT_THREADS = 32;

    private static final int TEST_DURATION_SECONDS = 60;

    private static final int TEXT_PERCENT = 70;

    private static final String[] WORDS = {
            "distributed", "system", "vector", "search", "grpc",
            "asynchronous", "java", "index", "lucene", "cloud",
            "scalable", "performance", "benchmark", "semantic",
            "architecture", "network", "service", "client", "server"
    };

    public static void main(String[] args) throws Exception {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(HOST, PORT)
                .usePlaintext()
                .build();

        SearchServiceGrpc.SearchServiceBlockingStub stub =
                SearchServiceGrpc.newBlockingStub(channel);

        preloadDocuments(stub, 50_000);

        System.out.println("Preload finished. Starting load test...");

        ExecutorService pool = Executors.newFixedThreadPool(CLIENT_THREADS);
        CountDownLatch stopLatch = new CountDownLatch(1);

        AtomicLong totalRequests = new AtomicLong();
        AtomicLong totalErrors = new AtomicLong();

        List<Long> latencies = new CopyOnWriteArrayList<>();

        Runnable worker = () -> {
            Random rnd = new Random();
            while (stopLatch.getCount() > 0) {
                long start = System.nanoTime();
                try {
                    int p = rnd.nextInt(100);
                    if (p < TEXT_PERCENT) {
                        doTextSearch(stub, rnd);
                    } else {
                        doVectorSearch(stub, rnd);
                    }
                    long end = System.nanoTime();
                    latencies.add(end - start);
                    totalRequests.incrementAndGet();
                } catch (Exception e) {
                    totalErrors.incrementAndGet();
                }
            }
        };

        for (int i = 0; i < CLIENT_THREADS; i++) {
            pool.submit(worker);
        }

        long testStart = System.currentTimeMillis();
        Thread.sleep(TEST_DURATION_SECONDS * 1000L);
        long testEnd = System.currentTimeMillis();

        stopLatch.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        channel.shutdownNow();

        long durationMs = testEnd - testStart;
        long requests = totalRequests.get();
        long errors = totalErrors.get();

        double rps = requests * 1000.0 / durationMs;

        System.out.println("===== LOAD TEST RESULT =====");
        System.out.println("Duration: " + durationMs + " ms");
        System.out.println("Threads: " + CLIENT_THREADS);
        System.out.println("Total requests: " + requests);
        System.out.println("Total errors: " + errors);
        System.out.printf("RPS: %.2f%n", rps);

        if (!latencies.isEmpty()) {
            latencies.sort(Long::compare);

            long p50 = percentile(latencies, 50);
            long p95 = percentile(latencies, 95);
            long p99 = percentile(latencies, 99);

            System.out.println("Latency (TEXT+VECTOR mix), micros:");
            System.out.println("p50: " + TimeUnit.NANOSECONDS.toMicros(p50));
            System.out.println("p95: " + TimeUnit.NANOSECONDS.toMicros(p95));
            System.out.println("p99: " + TimeUnit.NANOSECONDS.toMicros(p99));
        }
    }

    private static void preloadDocuments(SearchServiceGrpc.SearchServiceBlockingStub stub,
                                         int count) {
        Random rnd = new Random(42);
        for (int i = 0; i < count; i++) {
            String id = "preload-" + i;
            String title = "Document " + i;
            String content = randomText(rnd, 50);

            AddDocumentRequest req = AddDocumentRequest.newBuilder()
                    .setId(id)
                    .setTitle(title)
                    .setContent(content)
                    .build();

            stub.addDocument(req);
        }
    }

    private static void doTextSearch(SearchServiceGrpc.SearchServiceBlockingStub stub,
                                     Random rnd) {
        String q = WORDS[rnd.nextInt(WORDS.length)] + " "
                + WORDS[rnd.nextInt(WORDS.length)];

        SearchRequest req = SearchRequest.newBuilder()
                .setQuery(q)
                .setMethod(SearchMethod.TEXT)
                .build();

        stub.search(req);
    }

    private static void doVectorSearch(SearchServiceGrpc.SearchServiceBlockingStub stub,
                                       Random rnd) {
        String q = randomText(rnd, 10);

        SearchRequest req = SearchRequest.newBuilder()
                .setQuery(q)
                .setMethod(SearchMethod.VECTOR)
                .build();

        stub.search(req);
    }

    private static String randomText(Random rnd, int words) {
        StringBuilder sb = new StringBuilder(words * 8);
        for (int i = 0; i < words; i++) {
            sb.append(WORDS[rnd.nextInt(WORDS.length)]).append(' ');
        }
        return sb.toString();
    }

    private static long percentile(List<Long> sortedLatencies, int p) {
        if (sortedLatencies.isEmpty()) return 0L;
        int idx = (int) Math.ceil(p / 100.0 * sortedLatencies.size()) - 1;
        idx = Math.max(0, Math.min(idx, sortedLatencies.size() - 1));
        return sortedLatencies.get(idx);
    }
}
