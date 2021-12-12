package org.ericace.binary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Uses the AWS SDK V2 SdkAsyncHttpClient and S3AsyncClient classes for Netty event-driven I/O.
 */
public class S3AsyncBinaryProvider implements BinaryProvider {

    private static final Logger logger = LogManager.getLogger(S3AsyncBinaryProvider.class);

    /**
     * How we talk to AWS
     */
    private final S3AsyncClient client;

    /**
     * A temp dir to download objects from S3 into
     */
    private final String tmpDir;

    /**
     * A list of keys. These will be used by the {@link #getBinary} method instead of the key passed to that
     * method.
     */
    private final List<String> keys;

    /**
     * The bucket name from which to get an object
     */
    private final String bucketName;

    /**
     * Constructor
     *
     * @param maxConcurrency               Configures the <code>NettyNioAsyncHttpClient</code> connection pool
     * @param maxPendingConnectionAcquires Configures the <code>NettyNioAsyncHttpClient</code> max pending
     *                                     connection acquires.
     * @param bucketName                   The bucket name
     * @param regionStr                    The region - has to match the bucket
     * @param tmpDir                       A temp dir to download S3 objects from. (The class removes the object
     *                                     as soon as its input stream is closed.)
     * @param keys                         A list of keys from which to randomly select objects to download.
     *                                     (See {@link #getBinary}.)
     */
    public S3AsyncBinaryProvider(int maxConcurrency, int maxPendingConnectionAcquires, String bucketName,
                                 String regionStr, String tmpDir, List<String> keys) {
        this.tmpDir = tmpDir;
        this.keys = keys;
        this.bucketName = bucketName;

        SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
                .maxConcurrency(maxConcurrency)
                .maxPendingConnectionAcquires(maxPendingConnectionAcquires)
                .build();

        client = S3AsyncClient.builder()
                .region(Region.of(regionStr))
                .httpClient(httpClient)
                .build();
    }

    /**
     * Gets a binary randomly selected from the class {@link #keys} field.
     *
     * @param key IGNORED
     * @return The object from the S3 bucket.
     */
    @Override
    public BinaryObject getBinary(String key) {
        // TODO not guaranteed to avoid collisions and transfer manager will throw on file exists
        Path tmpFile = Paths.get(tmpDir, "tmp-" + Thread.currentThread().getId() + "-" +
                ThreadLocalRandom.current().nextLong(Long.MAX_VALUE) + ".bin").toAbsolutePath();
        int randomKey = keys.size() == 1 ? 0 : ThreadLocalRandom.current().nextInt(0, keys.size());
        String object = keys.get(randomKey);
        try {
            logger.info("Getting object for key {}", object);

            GetObjectRequest objectRequest = GetObjectRequest.builder().bucket(bucketName).key(object).build();
            CompletableFuture<GetObjectResponse> future = client.getObject(objectRequest,
                    AsyncResponseTransformer.toFile(tmpFile));
            future.whenComplete((resp, err) -> {
                if (resp == null) {
                    logger.error("Could not get binary: " + object + ". Cause: " + err.getMessage());
                    throw new RuntimeException("Could not get binary: " + object);
                }
            });
            future.join();
            return new LocalFileBinaryObject(tmpFile);
        } catch (Exception e) {
            logger.error("Could not get binary: " + object + ". Cause: " + e.getMessage());
            throw new RuntimeException("Could not get binary: " + object);
        }
    }
}
