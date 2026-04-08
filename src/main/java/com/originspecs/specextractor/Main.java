package com.originspecs.specextractor;

import com.originspecs.specextractor.client.DeepLClient;
import com.originspecs.specextractor.config.CliException;
import com.originspecs.specextractor.config.CliParser;
import com.originspecs.specextractor.config.Config;
import com.originspecs.specextractor.orchestration.Orchestrator;
import com.originspecs.specextractor.processor.CommonNameCorrector;
import com.originspecs.specextractor.processor.SpecProcessor;
import com.originspecs.specextractor.processor.SpecRecordPostProcessor;
import com.originspecs.specextractor.reader.WorkbookReader;
import com.originspecs.specextractor.reader.WorkbookReaderImpl;
import com.originspecs.specextractor.service.TranslationService;
import com.originspecs.specextractor.writer.JsonWriter;
import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpClient;
import java.util.List;

@Slf4j
public class Main {

    public static void main(String[] args) {
        try {
            Config config = CliParser.parseOrExit(args);
            Orchestrator orchestrator = createOrchestrator(config);
            orchestrator.execute(config);
        } catch (CliException e) {
            System.exit(1);
        } catch (Exception e) {
            log.error("Spec extraction failed", e);
            System.exit(1);
        }
    }

    private static Orchestrator createOrchestrator(Config config) {
        WorkbookReader reader = new WorkbookReaderImpl();
        HttpClient httpClient = HttpClient.newHttpClient();
        TranslationService translationService = new TranslationService(
                new DeepLClient(config.deeplApiKey(), httpClient));
        var processor = new SpecProcessor();
        List<SpecRecordPostProcessor> recordPostProcessors = List.of(CommonNameCorrector.create());
        var writer = new JsonWriter();
        return new Orchestrator(reader, translationService, processor, recordPostProcessors, writer);
    }
}
