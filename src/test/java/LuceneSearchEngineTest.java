import org.example.search.engine.LuceneSearchEngine;
import org.example.search.engine.SearchEngine;
import org.example.search.model.IndexedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LuceneSearchEngineTest {

    private LuceneSearchEngine engine;

    @BeforeEach
    void setUp() throws Exception {
        engine = new LuceneSearchEngine();
    }

    @Test
    void addSingleDocument_andSearchByBodyToken() throws Exception {
        IndexedDocument doc = new IndexedDocument(
                "1",
                "My first document",
                "This is a test body with lucene token"
        );

        engine.addDocument(doc);

        List<SearchEngine.SearchResultItem> results = engine.search("lucene", 10);

        assertNotNull(results);
        assertFalse(results.isEmpty(), "Expected at least one result");

        SearchEngine.SearchResultItem first = results.get(0);
        assertEquals("1", first.id());
        assertEquals("My first document", first.title());
        assertTrue(first.score() > 0.0f);
    }

    @Test
    void addMultipleDocuments_searchReturnsMostRelevantFirst() throws Exception {
        engine.addDocument(new IndexedDocument(
                "1",
                "Doc 1",
                "apple banana"
        ));
        engine.addDocument(new IndexedDocument(
                "2",
                "Doc 2",
                "apple apple banana"
        ));
        engine.addDocument(new IndexedDocument(
                "3",
                "Doc 3",
                "banana only"
        ));

        List<SearchEngine.SearchResultItem> results = engine.search("apple", 10);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.size() >= 2);

        assertEquals("2", results.get(0).id(), "Doc 2 should be the most relevant for 'apple'");
    }

    @Test
    void searchUnknownTerm_returnsEmptyList() throws Exception {
        engine.addDocument(new IndexedDocument(
                "1",
                "Doc",
                "some text in body"
        ));

        List<SearchEngine.SearchResultItem> results = engine.search("nonexistentterm123", 10);

        assertNotNull(results);
        assertTrue(results.isEmpty(), "Expected empty list for unknown term");
    }

    @Test
    void searchRespectsLimit() throws Exception {
        for (int i = 1; i <= 5; i++) {
            engine.addDocument(new IndexedDocument(
                    String.valueOf(i),
                    "Doc " + i,
                    "common body token"
            ));
        }

        List<SearchEngine.SearchResultItem> results = engine.search("common", 2);

        assertNotNull(results);
        assertEquals(2, results.size(), "Search should respect limit parameter");
    }

    @Test
    void indexIsPersistentWithinInstance() throws Exception {
        engine.addDocument(new IndexedDocument(
                "1",
                "Title 1",
                "body one"
        ));
        engine.addDocument(new IndexedDocument(
                "2",
                "Title 2",
                "body two"
        ));

        List<SearchEngine.SearchResultItem> firstSearch = engine.search("body", 10);
        assertEquals(2, firstSearch.size());

        engine.addDocument(new IndexedDocument(
                "3",
                "Title 3",
                "body three"
        ));

        List<SearchEngine.SearchResultItem> secondSearch = engine.search("body", 10);
        assertEquals(3, secondSearch.size());
    }
}
