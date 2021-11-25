package org.ericace;

import org.ericace.binary.*;
import org.ericace.threaded.ThreadedArchiveCreator;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A factory to create {@link ArchiveCreator} instances from configuration info specified on the command line.
 */
public class ArchiveCreatorFactory {
    /**
     * Creates an <code>ArchiveCreator</code> instance from command-line params
     * @param args The type of archive creator specified on the command line
     * @return the created instance
     */
    public static ArchiveCreator fromArgs(Args args) {
        DocumentReader reader = new DocumentReader(args.documentCount);
        BinaryProvider provider = null;
        Path binCache = Paths.get(System.getProperty("java.io.tmpdir"), "binaries");
        switch (args.binaryProvider) {
            case fake:
                provider = new FakeBinaryProvider(args.binarySizes);
                break;
            case s3client:
                provider = new AmazonS3BinaryProvider(args.bucketName, args.region, binCache.toAbsolutePath()
                        .toString(), args.keys);
                break;
            case transfermanager:
                provider = new S3TransferManagerBinaryProvider(args.threadCount, args.bucketName, args.region,
                        binCache.toAbsolutePath().toString(), args.keys);
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
                    .binaryService(new BinaryService(provider))
                    .reader(reader)
                    .tarFQPN(args.archiveFqpn)
                    .metrics(metrics)
                    .build();
        }
    }
}
