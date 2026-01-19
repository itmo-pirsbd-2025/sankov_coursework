package org.example.search.engine;


import org.example.search.model.IndexedDocument;

import java.util.List;

public interface SearchEngine {

    void addDocument(IndexedDocument doc) throws Exception;

    List<SearchResultItem> search(String query, int limit) throws Exception;

    record SearchResultItem(String id, String title, float score) {}
}
