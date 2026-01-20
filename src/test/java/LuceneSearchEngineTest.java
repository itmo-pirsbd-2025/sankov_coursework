import org.example.search.engine.LuceneSearchEngine;
import org.example.search.engine.SearchEngine;
import org.example.search.model.IndexedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LuceneSearchEngineTest {  // короткое имя!

    LuceneSearchEngine eng;  // короткие имена

    @BeforeEach
    void before() throws Exception {
        eng = new LuceneSearchEngine();
    }

    @Test
    void testOneDoc() throws Exception {
        IndexedDocument d1 = new IndexedDocument("1", "My doc", "hello lucene test");
        eng.addDocument(d1);

        List<SearchEngine.SearchResultItem> res = eng.search("lucene", 10);

        assertNotNull(res);
        assertFalse(res.isEmpty());

        var first = res.get(0);
        assertEquals("1", first.id());
        assertEquals("My doc", first.title());
        assertTrue(first.score() > 0);
    }

    @Test
    void testManyDocs() throws Exception {
        eng.addDocument(new IndexedDocument("1", "D1", "apple"));
        eng.addDocument(new IndexedDocument("2", "D2", "apple apple"));
        eng.addDocument(new IndexedDocument("3", "D3", "banana"));

        var res = eng.search("apple", 10);

        assertNotNull(res);
        assertTrue(res.size() >= 2);
        assertEquals("2", res.get(0).id());  // D2 больше apple
    }

    @Test
    void testNoResults() throws Exception {
        eng.addDocument(new IndexedDocument("1", "Doc", "normal text"));

        var res = eng.search("xyz123nonexistent", 10);
        assertTrue(res.isEmpty());
    }

    @Test
    void testLimit() throws Exception {
        // добавляем 5 доков
        for (int i = 1; i <= 5; i++) {
            eng.addDocument(new IndexedDocument(
                    "" + i,
                    "Doc" + i,
                    "test token"
            ));
        }

        var res = eng.search("test", 2);
        assertEquals(2, res.size());
    }

    @Test
    void testPersistent() throws Exception {
        eng.addDocument(new IndexedDocument("1", "T1", "body"));
        var first = eng.search("body", 10);
        assertEquals(1, first.size());
        eng.addDocument(new IndexedDocument("2", "T2", "body"));
        var second = eng.search("body", 10);
        assertEquals(2, second.size());
    }
}
