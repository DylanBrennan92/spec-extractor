package com.originspecs.specextractor.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DeepLClient {

    private final String apiKey;
    private final String apiUrl = "https://api-free.deepl.com";
    private final HttpClient httpClient;

    public DeepLClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }
}
