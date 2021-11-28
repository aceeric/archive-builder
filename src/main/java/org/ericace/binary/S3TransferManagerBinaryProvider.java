package org.ericace.binary;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Performs the same function as the {@link AmazonS3BinaryProvider} class, except gets S3 binaries using the
 * {@link TransferManager} AWS SDK class, which supports the ability to define a custom executor pool. However,
 * since this is ultimately rate-limited by AWS, it didn't have any performance impact over the
 * {@link AmazonS3BinaryProvider} class. Amazon rate-limits S3 GET operations to 5,500 per second / per prefix.
 * <p>
 * (See https://docs.aws.amazon.com/AmazonS3/latest/userguide/optimizing-performance.html)
 *
 * TODO: Create a bucket with enough unique keys to defeat the rate limit and test performance
 */
public class S3TransferManagerBinaryProvider implements BinaryProvider {

    private static final Logger logger = LogManager.getLogger(S3TransferManagerBinaryProvider.class);

    /**
     * How we talk to AWS
     */
    private final TransferManager transferManager;

    /**
     * The bucket name from which to get an object
     */
    private final String bucketName;

    /**
     * A list of keys. These will be used by the {@link #getBinary} method instead of the key passed to that
     * method.
     */
    private final List<String> keys;

    /**
     * A temp dir to download objects from S3 into
     */
    private final String tmpDir;

    /**
     * Constructor
     *
     * @param threads    The number of threads to allocate to the transfer manager thread pool
     * @param bucketName The bucket name
     * @param regionStr  The region - has to match the bucket
     * @param tmpDir     A temp dir to download S3 objects from. (The class removes the object as soon as its
     *                   input stream is closed.)
     * @param keys       A list of keys from which to randomly select objects to download. (See {@link #getBinary}.)
     */
    public S3TransferManagerBinaryProvider(int threads, String bucketName, String regionStr, String tmpDir, List<String> keys) {
        this.bucketName = bucketName;
        this.tmpDir = tmpDir;
        this.keys = keys;

        Regions region = Regions.fromName(regionStr);
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard().withRegion(region);
        AmazonS3 s3 = builder.build();

        transferManager = TransferManagerBuilder.standard()
                .withExecutorFactory(() -> Executors.newFixedThreadPool(threads))
                .withShutDownThreadPools(true)
                .withS3Client(s3)
                .build();
    }

    @Override
    public BinaryObject getBinary(String key) {
        // TODO not guaranteed to avoid collisions and transfer manager will throw on file exists
        Path tmpFile = Paths.get(tmpDir, "tmp-" + Thread.currentThread().getId() + "-" +
                ThreadLocalRandom.current().nextLong(Long.MAX_VALUE) + ".bin").toAbsolutePath();
        try {
            int randomKey = keys.size() == 1 ? 0 : ThreadLocalRandom.current().nextInt(0, keys.size());
            logger.info("Getting object for key {}", key);
            Download d = transferManager.download(bucketName, keys.get(randomKey), tmpFile.toFile());
            d.waitForCompletion();
        } catch (Exception e) {
            logger.error("Could not get binary");
            throw new RuntimeException("Could not get binary: " + key);
        }
        return new LocalFileBinaryObject(tmpFile);
    }

    @Override
    public void shutDownNow() {
        transferManager.shutdownNow(true);
    }
}
