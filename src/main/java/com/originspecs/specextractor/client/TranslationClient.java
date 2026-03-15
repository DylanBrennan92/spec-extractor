package com.originspecs.specextractor.client;

import com.originspecs.specextractor.model.TranslationRequest;
import com.originspecs.specextractor.model.TranslationResponse;
import com.originspecs.specextractor.model.UsageResponse;

/** Contract for translation API clients (e.g. DeepL). */
public interface TranslationClient {

    TranslationResponse translate(TranslationRequest request);

    UsageResponse getUsage();
}
