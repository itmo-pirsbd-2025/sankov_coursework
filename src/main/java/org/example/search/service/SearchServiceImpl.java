package org.example.search.service;

import io.grpc.stub.StreamObserver;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.KnnVectorQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.example.search.*;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SearchServiceImpl extends SearchServiceGrpc.SearchServiceImplBase {

    StandardAnalyzer analyzer;
    ByteBuffersDirectory dir;
    IndexWriter writer;
    ArrayList<Document> docQueue = new ArrayList<>();

    ScheduledExecutorService saverThread;
    int saveCounter = 0;

    public SearchServiceImpl() {
        try {
            analyzer = new StandardAnalyzer();
            dir = new ByteBuffersDirectory();
            writer = new IndexWriter(dir, new IndexWriterConfig(analyzer));
            saverThread = Executors.newSingleThreadScheduledExecutor();
            saverThread.scheduleAtFixedRate(() -> {
                saveCounter++;
                System.out.println("=== SAVE #" + saveCounter + " ===");
                saveAllDocs();
            }, 3, 3, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addDocument(AddDocumentRequest req, StreamObserver<AddDocumentResponse> resp) {
        try {
            Document doc = new Document();
            doc.add(new StringField("id", req.getId(), Field.Store.YES));
            doc.add(new TextField("title", req.getTitle(), Field.Store.YES));
            doc.add(new TextField("content", req.getContent(), Field.Store.YES));

            float[] vec = myVector(req.getTitle() + " " + req.getContent());
            doc.add(new KnnVectorField("vec", vec));

            docQueue.add(doc);

            resp.onNext(AddDocumentResponse.newBuilder().setSuccess(true).build());
            resp.onCompleted();
        } catch (Exception e) {
            resp.onError(e);
        }
    }

    @Override
    public void search(SearchRequest req, StreamObserver<SearchResponse> resp) {
        try {
            DirectoryReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);

            ArrayList<SearchResult> results = new ArrayList<>();

            if (req.getMethod() == SearchMethod.TEXT) {
                Query textQuery = new TermQuery(new Term("content", req.getQuery()));
                TopDocs hits = searcher.search(textQuery, 10);

                for (ScoreDoc hit : hits.scoreDocs) {
                    Document d = searcher.doc(hit.doc);
                    results.add(SearchResult.newBuilder()
                            .setId(d.get("id"))
                            .setTitle(d.get("title"))
                            .setSnippet(cutText(d.get("content")))
                            .build());
                }
            } else {
                float[] qvec = myVector(req.getQuery());
                Query vecQuery = new KnnVectorQuery("vec", qvec, 10);
                TopDocs hits = searcher.search(vecQuery, 10);

                System.out.println("vector search: " + hits.scoreDocs.length + " совпадений");

                for (ScoreDoc hit : hits.scoreDocs) {
                    Document d = searcher.doc(hit.doc);
                    results.add(SearchResult.newBuilder()
                            .setId(d.get("id"))
                            .setTitle(d.get("title"))
                            .setSnippet(cutText(d.get("content")))
                            .build());
                }
            }

            resp.onNext(SearchResponse.newBuilder().addAllResults(results).build());
            resp.onCompleted();
            reader.close();
        } catch (Exception e) {
            resp.onError(e);
        }
    }

    private void saveAllDocs() {
        try {
            if (docQueue.isEmpty()) {
                System.out.println("нет документов");
                return;
            }
            ArrayList<Document> copy = new ArrayList<>(docQueue);
            docQueue.clear();

            System.out.println("сохраняю " + copy.size() + " док-ов");

            for (int i = 0; i < copy.size(); i++) {
                writer.addDocument(copy.get(i));
            }
            writer.commit();

        } catch (Exception e) {
            System.out.println("save error: " + e.getMessage());
        }
    }
    private float[] myVector(String text) {
        float[] vec = new float[128];
        int h = text.hashCode();
        for (int i = 0; i < 128; i++) {
            vec[i] = ((h * 123 + i * 456) % 10000) / 5000.0f - 1.0f;
        }
        return vec;
    }

    private String cutText(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}
