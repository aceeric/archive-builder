package org.ericace.binary;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gets binaries from an S3 bucket. Since this project is basically a load and performance
 * tester, the class is used as follows in the project:
 * <ol>
 *     <li>Caller initializes the class with a list of keys that are in a bucket</li>
 *     <li>When the {@link #getBinary} method is called, the method ignores the <code>key</code> param and randomly
 *         selects a key from the instance list. So if you want to test with a uniform file size, initialize the list
 *         with one key. Otherwise initialize the list with keys representing S3 objects of varying size.</li>
 * </ol>
 */
public class AmazonS3BinaryProvider implements BinaryProvider {

    private static final Logger logger = LogManager.getLogger(AmazonS3BinaryProvider.class);

    /**
     * How we talk to AWS
     */
    private final AmazonS3 s3;

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
     * Constructor. Creates the instance from params.
     *
     * @param bucketName The bucket name
     * @param regionStr  The region - has to match the bucket
     * @param tmpDir     A temp dir to download S3 objects from. (The class removes the object as soon as its
     *                   input stream is closed.)
     * @param keys       A list of keys from which to randomly select objects to download. (See {@link #getBinary}.)
     */
    public AmazonS3BinaryProvider(String bucketName, String regionStr, String tmpDir, List<String> keys) {
        this.bucketName = bucketName;
        this.tmpDir = tmpDir;
        this.keys = keys;
        Regions region = Regions.fromName(regionStr);
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard().withRegion(region);
        s3 = builder.build();
        if (!Files.exists(Paths.get(tmpDir)) || !Files.isDirectory(Paths.get(tmpDir))) {
            throw new RuntimeException("Temp dir does not exist or is not a directory: " + tmpDir);
        }
    }

    /**
     * Ignores the key and gets a binary from the S3 instance bucket using a random key name from the instance
     * <code>ArrayList</code> {@link #keys}.
     *
     * @param key Ignored. (This class is a test class that randomizes object downloads.)
     * @return the Object
     */
    @Override
    public BinaryObject getBinary(String key) {
        File binFile;
        try {
            int randomKey = keys.size() == 1 ? 0 : ThreadLocalRandom.current().nextInt(0, keys.size());
            logger.info("Getting object for key {}", key);
            S3Object o = s3.getObject(bucketName, keys.get(randomKey));
            binFile = File.createTempFile("aws", ".bin", new File(tmpDir));
            try (S3ObjectInputStream s3is = o.getObjectContent(); FileOutputStream fos = new FileOutputStream(binFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = s3is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            }
        } catch (AmazonServiceException | IOException e) {
            logger.error("Could not get binary");
            throw new RuntimeException("Could not get binary: " + key);
        }
        return new LocalFileBinaryObject(binFile);
    }

    @Override
    public void shutDownNow() {
        // NOP
    }
}
