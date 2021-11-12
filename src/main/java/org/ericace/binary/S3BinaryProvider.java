package org.ericace.binary;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class S3BinaryProvider implements BinaryProvider {

    private final AmazonS3 s3;

    /**
     * The bucket name from which to get an object
     */
    private final String bucketName;

    /**
     * a temp dir to download objects from S3 into
     */
    private final String tmpDir;

    public S3BinaryProvider(String bucketName, String regionStr, String tmpDir) {
        this.bucketName = bucketName;
        this.tmpDir = tmpDir;
        Regions region = Regions.fromName(regionStr);
        s3 = AmazonS3ClientBuilder.standard().withRegion(region).build();
        if (!Files.exists(Paths.get(tmpDir)) || !Files.isDirectory(Paths.get(tmpDir))) {
            throw new RuntimeException("Temp dir does not exist or is not a directory: " + tmpDir);
        }
    }

    @Override
    public BinaryObject getBinary(String key) {
        File binFile;
        try {
            S3Object o = s3.getObject(bucketName, key);
            binFile = File.createTempFile("aws", ".bin", new File(tmpDir));
            try (S3ObjectInputStream s3is = o.getObjectContent();
                 FileOutputStream fos = new FileOutputStream(binFile)) {
                byte[] buffer = new byte[1024];
                int len = 0;
                while ((len = s3is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            }
        } catch (AmazonServiceException | IOException e) {
            throw new RuntimeException("Could not get binary: " + key);
        }
        return new LocalFileBinaryObject(binFile);
    }
}
