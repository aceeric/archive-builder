package org.ericace;

import io.prometheus.client.exporter.HTTPServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ericace.binary.BinaryService;
import org.ericace.binary.FakeBinaryProvider;
import org.ericace.binary.S3BinaryProvider;
import org.ericace.threaded.ConcurrentArchiveCreator;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;


/**
 * Entry point
 */
public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    private static final String TAR_FQPN = "/tmp/foo.tar.gz";


    /**
     * Main method
     */
    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {

        if (Args.parseArgs(args)) {
            Args.showConfig();
        }

        //HTTPServer server = new HTTPServer.Builder().withPort(1234).build();
        //testSingleThreaded();
        //testMultiThreadedFake();
        //testMultiThreadedS3();
        //server.stop();
    }

    private static void testSingleThreaded() throws IOException {
        logger.info("Single Threaded");
        Metrics metrics = new Metrics();
        metrics.start();
        ArchiveCreator.createArchive(new DocumentReader(30), new BinaryService(
                new FakeBinaryProvider(100_000, 2_800_000)), TAR_FQPN);
        metrics.finishAndPrint();
    }

    //  ------- ----  --------  ------------  -------
    //  threads docs  memcache  elapsed       tar size
    //  ------- ----  --------  ------------  -------
    //  10      5k    10k       00:04:10.316
    //  50      5k    10k       00:00:50.189
    //  100     5k    10k       00:00:25.169
    //  200     5k    10k       00:00:12.722
    //  500     5k    10k       00:00:05.317
    //  1000    5k    10k       00:00:02.887
    //  2000    15k   10k       00:00:04.582
    //  2000    150k  100k      00:00:38.131
    //  4000    150k  100k      00:00:20.026  153,601,024
    private static void testMultiThreadedFake() throws InterruptedException, ExecutionException {
        logger.info("Multi threaded start");
        Metrics metrics = new Metrics();
        ConcurrentArchiveCreator creator = new ConcurrentArchiveCreator.Builder()
                .binaryLoaderThreads(1000)
                .memCacheSize(100_000)
                .reader(new DocumentReader(200_000))
                .binaryService(new BinaryService(new FakeBinaryProvider(1_000)))
                .tarFQPN(TAR_FQPN)
                .metrics(metrics)
                .build();
        metrics.start();
        creator.createArchive();
        metrics.finishAndPrint();
        logger.info("Multi threaded finish");
    }

    private static void testMultiThreadedS3() throws InterruptedException, ExecutionException {
        logger.info("Multi threaded S3 start");
        ArrayList<String> keys = new ArrayList<>(List.of("1000-bytes")); //, "10000-bytes")); //, "100000-bytes"));
        String bucket = System.getProperty("bucket");
        String region = System.getProperty("region");

        Metrics metrics = new Metrics();
        ConcurrentArchiveCreator creator = new ConcurrentArchiveCreator.Builder()
                .binaryLoaderThreads(100)
                .memCacheSize(200_000)
                .reader(new DocumentReader(50_000))
                .binaryService(new BinaryService(new S3BinaryProvider(bucket, region, "/tmp/binaries", keys)))
                .tarFQPN(TAR_FQPN)
                .metrics(metrics)
                .build();
        metrics.start();
        creator.createArchive();
        metrics.finishAndPrint();
        logger.info("Multi threaded S3 finish");
    }

}
