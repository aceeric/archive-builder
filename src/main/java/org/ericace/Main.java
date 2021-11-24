package org.ericace;

import io.prometheus.client.exporter.HTTPServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Main class
 */
public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    /**
     * Entry point
     */
    public static void main(String[] args) throws IOException {
        Args parsedArgs = Args.parse(args);
        if (parsedArgs == null) {
            return;
        } else if (parsedArgs.showConfig) {
            parsedArgs.showConfig();
            return;
        }
        logger.info("Starting");
        ArchiveCreator creator = ArchiveCreatorFactory.fromArgs(parsedArgs);
        try (HTTPServer server = new HTTPServer.Builder().withPort(parsedArgs.metricsPort).build()) {
            creator.getMetrics().start();
            creator.createArchive();
            creator.getMetrics().finishAndPrint();
        }
        logger.info("Exiting");
    }
}
