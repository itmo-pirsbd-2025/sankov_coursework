package org.example.search;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.example.search.service.SearchServiceImpl;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
@State(Scope.Benchmark)
public class SearchJmhBenchmark {

    ManagedChannel ch;
    SearchServiceGrpc.SearchServiceBlockingStub stub;
    Random rand = new Random(123);
    int nextId = 0;

    static final String[] words = {"grpc", "search", "lucene", "java", "test",
            "fast", "bench", "server", "client", "doc"};  // короче массив

    @Param({"100", "1000", "10000"})
    int docsCount;

    @Param({"100", "500"})
    int wordsCount;

    @Setup
    public void init() throws IOException {
        var server = NettyServerBuilder.forPort(50051)
                .addService(new SearchServiceImpl())
                .build().start();

        System.out.println("server started on 50051");

        ch = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext().build();

        stub = SearchServiceGrpc.newBlockingStub(ch);

        // warmup доки
        for (int i = 0; i < docsCount; i++) {
            stub.addDocument(AddDocumentRequest.newBuilder()
                    .setId("w-" + i)
                    .setTitle("W" + i)
                    .setContent(makeText(wordsCount))
                    .build());
        }
        System.out.println("added " + docsCount + " warmup docs");
    }

    @TearDown
    public void done() {
        if (ch != null) {
            ch.shutdownNow();
        }
    }

    @Benchmark
    public void addDoc() {
        var req = AddDocumentRequest.newBuilder()
                .setId("d" + nextId++)
                .setTitle("T" + nextId)
                .setContent(makeText(wordsCount))
                .build();
        stub.addDocument(req);
    }

    @Benchmark
    public void textSrch() {
        String q = words[rand.nextInt(words.length)] + " " +
                words[rand.nextInt(words.length)];

        stub.search(SearchRequest.newBuilder()
                .setQuery(q)
                .setMethod(SearchMethod.TEXT)
                .build());
    }

    @Benchmark
    public void vecSrch() {
        stub.search(SearchRequest.newBuilder()
                .setQuery(makeText(wordsCount/5))
                .setMethod(SearchMethod.VECTOR)
                .build());
    }

    private String makeText(int cnt) {
        StringBuilder sb = new StringBuilder();
        while (cnt-- > 0) {
            sb.append(words[rand.nextInt(words.length)]).append(' ');
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
