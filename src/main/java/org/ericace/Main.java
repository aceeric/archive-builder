package org.ericace;

import io.prometheus.client.exporter.HTTPServer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;
import java.util.List;

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
        if (parsedArgs.loggers.size() != 0) {
            configureAdditionalLoggers(parsedArgs.loggers);
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

    /**
     * The command-line allows specific classes to have their logging set to INFO for debugging purposes, so
     * you can run the compiled JAR and turn class logging on for specific classes at run-time without having to
     * modify or override the 'log4j2.xml' config file embedded in the JAR. In the log4j2.xml embedded in
     * the JAR, only the <code>Main</code> class and the <code>Metrics</code> class are set to support
     * INFO level logging. See the {@link Args} class for the command-line option to enable info logging for
     * a specific class or classes.
     *
     * @param loggers a list of classes, like "org.ericace.Main"
     */
    private static void configureAdditionalLoggers(List<String> loggers) {
        for (String logger : loggers) {
            Configurator.setLevel(logger, Level.INFO);
        }
    }
}
