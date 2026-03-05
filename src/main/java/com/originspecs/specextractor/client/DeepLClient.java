package com.originspecs.specextractor.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.originspecs.specextractor.config.Constants;
import com.originspecs.specextractor.model.TranslationRequest;
import com.originspecs.specextractor.model.TranslationResponse;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * HTTP client responsible for communicating with the DeepL translation API.
 * Serialises requests to JSON, sends them, and deserialises the response.
 * Throws {@link DeepLApiException} on any non-200 response.
 */
@Slf4j
public class DeepLClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String apiKey;
    private final HttpClient httpClient;

    public DeepLClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Sends a translation request to DeepL and returns the response.
     *
     * @param request The translation request containing texts and target language
     * @return The deserialized translation response
     * @throws DeepLApiException if the API returns a non-200 status code
     */
    public TranslationResponse translate(TranslationRequest request) {
        try {
            String requestBody = OBJECT_MAPPER.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(Constants.DEEPL_API_URL))
                    .header("Authorization", "DeepL-Auth-Key " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "spec-extractor")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            log.debug("Sending {} text(s) to DeepL", request.text().size());

            HttpResponse<String> httpResponse = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                throw new DeepLApiException(
                        "DeepL API returned status " + httpResponse.statusCode(),
                        httpResponse.statusCode(),
                        httpResponse.body());
            }

            TranslationResponse response = OBJECT_MAPPER.readValue(
                    httpResponse.body(), TranslationResponse.class);

            log.debug("Received {} translation(s) from DeepL", response.translations().size());
            return response;

        } catch (DeepLApiException e) {
            throw e;
        } catch (Exception e) {
            throw new DeepLApiException("Failed to contact DeepL API: " + e.getMessage(), -1, "");
        }
    }
}
