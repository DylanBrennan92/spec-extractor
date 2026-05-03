package com.originspecs.specextractor.service.translation;

import com.originspecs.specextractor.client.TranslationClient;
import com.originspecs.specextractor.config.Constants;
import com.originspecs.specextractor.model.TranslationRequest;
import com.originspecs.specextractor.model.TranslationResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Splits texts into API-sized batches, sends them via {@link TranslationClient}, and aggregates billing.
 */
@Slf4j
public final class TranslationBatchSender {

    private final TranslationClient client;
    private final int batchSize;

    public TranslationBatchSender(TranslationClient client, int batchSize) {
        this.client = Objects.requireNonNull(client, "client");
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive: " + batchSize);
        }
        this.batchSize = batchSize;
    }

    /**
     * Sends all texts in order; returned translations match {@code textsInOrder} one-for-one.
     */
    public TranslationBatchOutcome sendAll(List<String> textsInOrder) {
        if (textsInOrder.isEmpty()) {
            return new TranslationBatchOutcome(List.of(), 0);
        }

        List<List<String>> batches = splitIntoBatches(textsInOrder);
        log.info("Split into {} batch(es) of up to {} texts", batches.size(), batchSize);

        logUsageBefore();
        List<String> allTranslated = new ArrayList<>();
        int totalBilledThisRun = 0;

        for (int i = 0; i < batches.size(); i++) {
            log.debug("Sending batch {}/{}", i + 1, batches.size());

            TranslationRequest request = new TranslationRequest(
                    batches.get(i),
                    Constants.SOURCE_LANG,
                    Constants.TARGET_LANG,
                    Constants.DEEPL_CUSTOM_INSTRUCTIONS,
                    Constants.DEEPL_CONTEXT,
                    true);
            TranslationResponse response = client.translate(request);

            if (response.billedCharacters() != null) {
                totalBilledThisRun += response.billedCharacters();
            } else {
                for (TranslationResponse.Translation t : response.translations()) {
                    if (t.billedCharacters() != null) {
                        totalBilledThisRun += t.billedCharacters();
                    }
                }
            }
            response.translations().stream()
                    .map(TranslationResponse.Translation::text)
                    .forEach(allTranslated::add);
        }

        logUsageAfter(totalBilledThisRun);
        return new TranslationBatchOutcome(List.copyOf(allTranslated), totalBilledThisRun);
    }

    private List<List<String>> splitIntoBatches(List<String> texts) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            batches.add(new ArrayList<>(texts.subList(i, end)));
        }
        return batches;
    }

    private void logUsageBefore() {
        client.getUsage().ifPresent(usage ->
                log.info("DeepL quota before translation: {} chars remaining (used: {}/{})",
                        usage.charsRemaining(), usage.characterCount(), usage.characterLimit()));
    }

    private void logUsageAfter(int totalBilledThisRun) {
        client.getUsage().ifPresent(usage ->
                log.info("DeepL quota after translation: {} chars remaining (used: {}/{})",
                        usage.charsRemaining(), usage.characterCount(), usage.characterLimit()));
        log.info("Chars converted this run: {}", totalBilledThisRun);
    }
}
