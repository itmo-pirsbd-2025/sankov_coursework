package org.example.search.service;

import io.grpc.stub.StreamObserver;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.KnnVectorQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.example.search.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchServiceImpl extends SearchServiceGrpc.SearchServiceImplBase {

    private static final int BATCH_SIZE = 32;
    private static final long FLUSH_INTERVAL_MS = 500;

    private final ByteBuffersDirectory directory = new ByteBuffersDirectory();
    private final StandardAnalyzer analyzer = new StandardAnalyzer();
    private final IndexWriter writer;
    private final SearcherManager searcherManager;

    private final ExecutorService searchExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2,
            r -> {
                Thread t = new Thread(r, "search-executor-" + new AtomicInteger().incrementAndGet());
                t.setDaemon(true);
                return t;
            }
    );

    private final BlockingQueue<Document> ingestQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService ingestScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ingest-worker");
                t.setDaemon(true);
                return t;
            });

    public SearchServiceImpl() {
        try {
            writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
            searcherManager = new SearcherManager(writer, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ingestScheduler.scheduleWithFixedDelay(
                this::flushBatch,
                FLUSH_INTERVAL_MS,
                FLUSH_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void addDocument(AddDocumentRequest request, StreamObserver<AddDocumentResponse> responseObserver) {
        try {
            Document doc = new Document();
            doc.add(new StringField("id", request.getId(), Field.Store.YES));
            doc.add(new TextField("title", request.getTitle(), Field.Store.YES));
            doc.add(new TextField("content", request.getContent(), Field.Store.YES));
            doc.add(new KnnVectorField("embedding",
                    generateEmbedding(request.getTitle() + " " + request.getContent())));

            ingestQueue.offer(doc);

            responseObserver.onNext(AddDocumentResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void search(SearchRequest request, StreamObserver<SearchResponse> responseObserver) {
        searchExecutor.submit(() -> {
            IndexSearcher searcher = null;
            try {
                searcherManager.maybeRefresh();
                searcher = searcherManager.acquire();

                List<SearchResult> results = new ArrayList<>();

                if (request.getMethod() == SearchMethod.TEXT) {
                    Query q = new TermQuery(new Term("content", request.getQuery()));
                    TopDocs docs = searcher.search(q, 10);

                    for (ScoreDoc sd : docs.scoreDocs) {
                        Document d = searcher.doc(sd.doc);
                        results.add(SearchResult.newBuilder()
                                .setId(d.get("id"))
                                .setTitle(d.get("title"))
                                .setSnippet(d.get("content")
                                        .substring(0, Math.min(100, d.get("content").length())))
                                .build());
                    }
                } else {
                    float[] v = generateEmbedding(request.getQuery());
                    TopDocs docs = searcher.search(new KnnVectorQuery("embedding", v, 5), 5);

                    for (ScoreDoc sd : docs.scoreDocs) {
                        Document d = searcher.doc(sd.doc);
                        results.add(SearchResult.newBuilder()
                                .setId(d.get("id"))
                                .setTitle(d.get("title"))
                                .setSnippet(d.get("content")
                                        .substring(0, Math.min(100, d.get("content").length())))
                                .build());
                    }
                }

                responseObserver.onNext(SearchResponse.newBuilder().addAllResults(results).build());
                responseObserver.onCompleted();

            } catch (Exception e) {
                responseObserver.onError(e);
            } finally {
                if (searcher != null) {
                    try {
                        searcherManager.release(searcher);
                    } catch (IOException ignored) {}
                }
            }
        });
    }

    private void flushBatch() {
        try {
            List<Document> batch = new ArrayList<>(BATCH_SIZE);
            ingestQueue.drainTo(batch, BATCH_SIZE);

            if (batch.isEmpty()) return;

            writer.addDocuments(batch);
            writer.commit();
            searcherManager.maybeRefresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float[] generateEmbedding(String text) {
        String[] tokens = text.toLowerCase().split("\\W+");
        float[] v = new float[128];

        for (int i = 0; i < tokens.length; i++) {
            float[] w = getPretrainedWordVector(tokens[i]);
            for (int j = 0; j < v.length; j++) {
                v[j] += w[j];
            }
        }

        float norm = 0;
        for (float x : v) norm += x * x;
        norm = (float) Math.sqrt(norm);
        if (norm > 1e-6f) {
            for (int i = 0; i < v.length; i++) v[i] /= norm;
        }

        return v;
    }

    private float[] getPretrainedWordVector(String token) {
        float[] vec = new float[128];
        int h = token.hashCode();
        for (int i = 0; i < vec.length; i++) {
            vec[i] = (float) Math.sin((h + i * 31) & 0xFF);
        }
        return vec;
    }
}
