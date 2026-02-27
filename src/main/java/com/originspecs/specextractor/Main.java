package com.originspecs.specextractor;

import com.originspecs.specextractor.config.CliException;
import com.originspecs.specextractor.config.CliParser;
import com.originspecs.specextractor.config.Config;
import com.originspecs.specextractor.orchestration.SpecExtractorOrchestrator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static void main(String[] args) {
        try {
            Config config = CliParser.parseOrExit(args);
            SpecExtractorOrchestrator orchestrator = new SpecExtractorOrchestrator();
            orchestrator.execute(config);
        } catch (CliException e) {
            System.exit(1);
        } catch (Exception e) {
            log.error("Spec extraction failed", e);
            System.exit(1);
        }
    }
}
