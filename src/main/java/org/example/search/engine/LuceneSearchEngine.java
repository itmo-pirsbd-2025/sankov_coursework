package org.example.search.engine;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;
import org.example.search.model.IndexedDocument;

import java.util.*;

public class LuceneSearchEngine implements SearchEngine {

    StandardAnalyzer analizer;
    Directory dir;
    IndexWriter indexWriter;

    public LuceneSearchEngine() throws Exception {
        analizer = new StandardAnalyzer();
        dir = new ByteBuffersDirectory();

        IndexWriterConfig conf = new IndexWriterConfig(analizer);
        indexWriter = new IndexWriter(dir, conf);
    }

    public void addDocument(IndexedDocument doc) throws Exception {
        Document luceneDoc = new Document();

        // сохраняем id как строку
        luceneDoc.add(new StringField("id", doc.id(), Store.YES));
        luceneDoc.add(new TextField("title", doc.title(), Store.YES));
        luceneDoc.add(new TextField("content", doc.body(), Store.NO));

        indexWriter.addDocument(luceneDoc);
        indexWriter.commit();  // сохраняем изменения
    }

    public List<SearchResultItem> search(String queryStr, int maxResults) throws Exception {
        ArrayList<SearchResultItem> found = new ArrayList<>();

        DirectoryReader reader = DirectoryReader.open(dir);
        try {
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser qp = new QueryParser("content", analizer);

            Query q = qp.parse(queryStr);
            TopDocs hits = searcher.search(q, maxResults);

            for (ScoreDoc hit : hits.scoreDocs) {
                Document doc = searcher.doc(hit.doc);
                String docId = doc.get("id");
                String title = doc.get("title");
                float score = hit.score;

                found.add(new SearchResultItem(docId, title, score));
            }
        } finally {
            reader.close();
        }

        return found;
    }
}
