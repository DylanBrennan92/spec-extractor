package com.originspecs.specextractor;

import com.originspecs.specextractor.config.CliParser;
import com.originspecs.specextractor.config.Config;
import com.originspecs.specextractor.orchestration.SpecExtractorOrchestrator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static void main(String[] args) {
        Config config = CliParser.parseOrExit(args);

        try {
            SpecExtractorOrchestrator orchestrator = new SpecExtractorOrchestrator();
            orchestrator.execute(config);
        } catch (Exception e) {
            log.error("Spec extraction failed", e);
            System.exit(1);
        }
    }
}
