package org.example.search;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.example.search.service.SearchServiceImpl;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class SearchJmhBenchmark {

    private ManagedChannel channel;
    private SearchServiceGrpc.SearchServiceBlockingStub stub;
    private Random rnd;
    private int docId = 0;

    @Param({"100", "1000", "10000"})
    private int numDocs;

    @Param({"100", "500", "1000"})
    private int wordsPerDoc;

    private static final String[] WORDS = {
            "distributed", "system", "vector", "search", "grpc",
            "asynchronous", "java", "index", "lucene", "cloud",
            "scalable", "performance", "benchmark", "semantic",
            "architecture", "network", "service", "client", "server"
    };

    @Setup(Level.Trial)
    public void setup() throws IOException {
        Server server = NettyServerBuilder.forPort(50051)
                .addService(new SearchServiceImpl())
                .build()
                .start();

        channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        stub = SearchServiceGrpc.newBlockingStub(channel);
        rnd = new Random(42);

        for (int i = 0; i < numDocs; i++) {
            stub.addDocument(AddDocumentRequest.newBuilder()
                    .setId("warmup-" + i)
                    .setTitle("Warmup " + i)
                    .setContent(randomText(wordsPerDoc))
                    .build());
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    @Benchmark
    public void addDocument() {
        AddDocumentRequest req = AddDocumentRequest.newBuilder()
                .setId("doc-" + (docId++))
                .setTitle("Title " + docId)
                .setContent(randomText(wordsPerDoc))
                .build();

        stub.addDocument(req);
    }

    @Benchmark
    public void textSearch() {
        String q = WORDS[rnd.nextInt(WORDS.length)]
                + " " + WORDS[rnd.nextInt(WORDS.length)];

        SearchRequest req = SearchRequest.newBuilder()
                .setQuery(q)
                .setMethod(SearchMethod.TEXT)
                .build();

        stub.search(req);
    }

    @Benchmark
    public void vectorSearch() {
        String q = randomText(wordsPerDoc / 10);

        SearchRequest req = SearchRequest.newBuilder()
                .setQuery(q)
                .setMethod(SearchMethod.VECTOR)
                .build();

        stub.search(req);
    }

    private String randomText(int words) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words; i++) {
            sb.append(WORDS[rnd.nextInt(WORDS.length)]).append(' ');
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(new String[]{
                "org.example.search.SearchJmhBenchmark",
                "-wi", "3",
                "-i", "5",
                "-f", "1",
                "-bm", "avgt",
                "-tu", "ms"
        });
    }
}
