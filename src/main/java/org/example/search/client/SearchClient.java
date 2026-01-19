package org.example.search.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.search.*;

import java.util.List;

public class SearchClient {

    private final ManagedChannel channel;
    private final SearchServiceGrpc.SearchServiceBlockingStub blockingStub;

    public SearchClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = SearchServiceGrpc.newBlockingStub(channel);
    }

    public boolean addDocument(String id, String title, String content) {
        AddDocumentRequest request = AddDocumentRequest.newBuilder()
                .setId(id)
                .setTitle(title)
                .setContent(content)
                .build();
        AddDocumentResponse response = blockingStub.addDocument(request);
        return response.getSuccess();
    }

    public List<SearchResult> search(String query, SearchMethod method) {
        SearchRequest request = SearchRequest.newBuilder()
                .setQuery(query)
                .setMethod(method)
                .build();
        SearchResponse response = blockingStub.search(request);
        return response.getResultsList();
    }

    public void shutdown() {
        channel.shutdown();
    }
}
