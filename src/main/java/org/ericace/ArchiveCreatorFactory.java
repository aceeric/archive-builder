package org.ericace;

import org.ericace.binary.BinaryProvider;
import org.ericace.binary.BinaryService;
import org.ericace.binary.FakeBinaryProvider;
import org.ericace.binary.S3BinaryProvider;
import org.ericace.threaded.ThreadedArchiveCreator;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A factory to create {@link ArchiveCreator} instances from configuration info specified on the command line
 * that is encapsulated in an {@link Args} instance.
 */
public class ArchiveCreatorFactory {
    public static ArchiveCreator fromArgs(Args args) {
        DocumentReader reader = new DocumentReader(args.documentCount);
        BinaryProvider provider = null;
        if (args.binaryProvider == Args.BinaryProvider.fake) {
            provider = new FakeBinaryProvider(args.binarySizes);
        } else {
            Path binCache = Paths.get(System.getProperty("java.io.tmpdir"), "binaries");
            provider = new S3BinaryProvider(args.bucketName, args.region, binCache.toAbsolutePath().toString(), args.keys);
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
