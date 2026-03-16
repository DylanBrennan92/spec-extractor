package com.originspecs.specextractor.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.originspecs.specextractor.config.Constants;
import com.originspecs.specextractor.model.TranslationRequest;
import com.originspecs.specextractor.model.TranslationResponse;
import com.originspecs.specextractor.model.UsageResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.Optional;

/**
 * HTTP client responsible for communicating with the DeepL translation API.
 * Serialises requests to JSON, sends them, and deserialises the response.
 * Throws {@link DeepLApiException} on any non-200 response.
 */
@Slf4j
public class DeepLClient implements TranslationClient {

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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DeepLApiException("DeepL API request interrupted: " + e.getMessage(), -1, "", e);
        } catch (IOException e) {
            throw new DeepLApiException("Failed to contact DeepL API: " + e.getMessage(), -1, "", e);
        }
    }

    /**
     * Fetches current usage and quota from DeepL.
     *
     * @return Optional containing the usage response if the API call succeeded; empty if the
     *         request failed, returned non-200, or was interrupted
     */
    public Optional<UsageResponse> getUsage() {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(Constants.DEEPL_USAGE_URL))
                    .header("Authorization", "DeepL-Auth-Key " + apiKey)
                    .header("User-Agent", "spec-extractor")
                    .GET()
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                log.warn("DeepL usage API returned status {}", httpResponse.statusCode());
                return Optional.empty();
            }

            return Optional.of(OBJECT_MAPPER.readValue(httpResponse.body(), UsageResponse.class));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Could not fetch DeepL usage: interrupted");
            return Optional.empty();
        } catch (IOException e) {
            log.warn("Could not fetch DeepL usage: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
