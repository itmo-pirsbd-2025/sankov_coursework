package org.example.search.model;


public class IndexedDocument {
    private final String id;
    private final String title;
    private final String body;

    public IndexedDocument(String id, String title, String body) {
        this.id = id;
        this.title = title;
        this.body = body;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String body() {
        return body;
    }
}
