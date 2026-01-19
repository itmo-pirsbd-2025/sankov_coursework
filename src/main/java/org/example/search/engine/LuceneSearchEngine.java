package org.example.search.engine;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.example.search.model.IndexedDocument;

import java.util.ArrayList;
import java.util.List;

public class LuceneSearchEngine implements SearchEngine {

    private final StandardAnalyzer analyzer = new StandardAnalyzer();
    private final Directory directory = new ByteBuffersDirectory();
    private final IndexWriter writer;

    public LuceneSearchEngine() throws Exception {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        this.writer = new IndexWriter(directory, config);
    }

    @Override
    public synchronized void addDocument(IndexedDocument doc) throws Exception {
        Document d = new Document();
        d.add(new StringField("id", doc.id(), Field.Store.YES));
        d.add(new TextField("title", doc.title(), Field.Store.YES));
        d.add(new TextField("body", doc.body(), Field.Store.NO));

        writer.addDocument(d);
        writer.commit();
    }

    @Override
    public synchronized List<SearchResultItem> search(String query, int limit) throws Exception {
        try (DirectoryReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("body", analyzer);

            var q = parser.parse(query);
            var topDocs = searcher.search(q, limit);

            List<SearchResultItem> results = new ArrayList<>();
            for (ScoreDoc sd : topDocs.scoreDocs) {
                Document d = searcher.doc(sd.doc);
                results.add(new SearchResultItem(
                        d.get("id"),
                        d.get("title"),
                        sd.score
                ));
            }

            return results;
        }
    }
}
