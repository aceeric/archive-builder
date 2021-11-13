package org.ericace;

import org.ericace.binary.BinaryService;
import org.ericace.binary.FakeBinaryProvider;
import org.ericace.binary.S3BinaryProvider;
import org.ericace.threaded.ConcurrentArchiveCreator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Entry point
 * TODO LOOK AT https://stackoverflow.com/questions/54394042/java-how-to-avoid-using-thread-sleep-in-a-loop?noredirect=1&lq=1
 */
public class Main {

    private static final String TAR_FQPN = "/tmp/foo.tar.gz";

    /**
     * Main method
     */
    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        //Logger.classFilter(ArchiveCreator.class, ConcurrentArchiveCreator.InternalArchiveCreator.class);
        Logger.classFilter(S3BinaryProvider.class);
        //Logger.messageFilter("ook bin");
        //testSingleThreaded();
        //testMultiThreadedFake();
        testMultiThreadedS3();
    }

    private static void testSingleThreaded() throws IOException {
        Logger.log(Main.class, "Single Threaded");
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
        Logger.log(Main.class, "Multi Threaded");
        Metrics metrics = new Metrics();
        ConcurrentArchiveCreator creator = new ConcurrentArchiveCreator.Builder()
                .binaryLoaderThreads(4_000)
                .memCacheSize(200_000)
                .reader(new DocumentReader(5_000))
                .binaryService(new BinaryService(new FakeBinaryProvider(100_000, 2_800_000)))
                .tarFQPN(TAR_FQPN)
                .build();
        metrics.start();
        creator.createArchive();
        metrics.finishAndPrint();
    }

    private static void testMultiThreadedS3() throws InterruptedException, ExecutionException {
        Logger.log(Main.class, "Multi Threaded S3");
        ArrayList<String> keys = new ArrayList<>(List.of("1000-bytes", "10000-bytes", "100000-bytes"));
        String bucket = System.getProperty("bucket");
        String region = System.getProperty("region");

        Metrics metrics = new Metrics();
        ConcurrentArchiveCreator creator = new ConcurrentArchiveCreator.Builder()
                .binaryLoaderThreads(1)
                .memCacheSize(100000)
                .reader(new DocumentReader(5000))
                .binaryService(new BinaryService(new S3BinaryProvider(bucket, region, "/tmp/binaries", keys)))
                .tarFQPN(TAR_FQPN)
                .metrics(metrics)
                .build();
        metrics.start();
        creator.createArchive();
        metrics.finishAndPrint();
    }
}
