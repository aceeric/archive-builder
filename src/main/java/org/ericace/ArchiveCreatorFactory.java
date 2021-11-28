package org.ericace;

import org.ericace.binary.*;
import org.ericace.threaded.ThreadedArchiveCreator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A factory to create {@link ArchiveCreator} instances from configuration info specified on the command line.
 */
public class ArchiveCreatorFactory {
    /**
     * Creates an <code>ArchiveCreator</code> instance from command-line params
     *
     * @param args Archive creator configuration specified on the command line
     * @return the created instance
     */
    public static ArchiveCreator fromArgs(Args args) {
        // just create a dummy document reader that returns 'file-1', 'file-2', ...
        DocumentReader reader = new DocumentReader(args.documentCount);
        BinaryProvider provider = null;
        switch (args.binaryProvider) {
            case fake:
                provider = new FakeBinaryProvider(args.binarySizes);
                break;
            case s3client:
                provider = new AmazonS3BinaryProvider(args.bucketName, args.region, getOrCreateBinCachePath(),
                        args.keys);
                break;
            case transfermanager:
                provider = new S3TransferManagerBinaryProvider(args.threadCount, args.bucketName, args.region,
                        getOrCreateBinCachePath(), args.keys);
                break;
        }
        Metrics metrics = new Metrics();
        if (args.scenario == Args.Scenario.multi) {
            return new ThreadedArchiveCreator.Builder()
                    .binaryLoaderThreads(args.threadCount)
                    .memCacheSize(args.cacheSize)
                    .reader(reader)
                    .binaryService(new BinaryService(provider))
                    .tarFQPN(args.archiveFqpn)
                    .metrics(metrics)
                    .build();
        } else {
            return new SingleThreadArchiveCreator.Builder()
                    .reader(reader)
                    .binaryService(new BinaryService(provider))
                    .tarFQPN(args.archiveFqpn)
                    .metrics(metrics)
                    .build();
        }
    }

    /**
     * Ensures that a directory named 'binaries' exists in the system TEMP directory to hold
     * downloaded binary objects. In the design, binaries downloaded from S3 are encapsulated in a
     * {@link LocalFileBinaryObject}, which provides an input stream reader over the object contents
     * that deletes the object from the file system when the stream is closed.
     *
     * @return The absolute path to the directory
     */
    private static String getOrCreateBinCachePath() {
        Path binCache = Paths.get(System.getProperty("java.io.tmpdir"), "binaries");
        if (Files.isDirectory(binCache)) {
            return binCache.toAbsolutePath().toString();
        } else if (Files.isRegularFile(binCache)) {
            throw new RuntimeException("Can't create binary cache directory because of filename collision");
        }
        try {
            Files.createDirectory(binCache);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return binCache.toAbsolutePath().toString();
    }
}
