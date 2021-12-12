package org.ericace;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.compress.utils.IOUtils;
import org.ericace.binary.*;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * These aren't tests, they're just for debugging
 */
public class AppTest 
{
    private static final String BUCKET_PROPERTY = "bucket";
    private static final String REGION_PROPERTY = "region";

    private static final String BUCKET = System.getProperty(BUCKET_PROPERTY);
    private static final String REGION = System.getProperty(REGION_PROPERTY);

    @Ignore
    @Test
    public void testAmazonS3BinaryProvider() throws IOException {
        assertNotNull(BUCKET);
        assertNotNull(REGION);
        ArrayList<String> keys = new ArrayList<>(List.of("1000-bytes"));
        AmazonS3BinaryProvider p = new AmazonS3BinaryProvider(BUCKET, REGION, "/tmp", keys);
        BinaryObject obj = p.getBinary("IGNORED");
        try (InputStream ois = obj.getInputStream()) {
            IOUtils.copy(ois, System.out);
            System.out.println("Done");
        }
    }

    @Ignore
    @Test
    public void testS3TransferManagerBinaryProvider() throws IOException {
        assertNotNull(BUCKET);
        assertNotNull(REGION);
        ArrayList<String> keys = new ArrayList<>(List.of("1000-bytes"));
        S3TransferManagerBinaryProvider p = new S3TransferManagerBinaryProvider(1, BUCKET, REGION, "/tmp", keys);
        BinaryObject obj = p.getBinary("IGNORED");
        System.out.println("FILE=" + ((LocalFileBinaryObject)obj).getAbsolutePath());
        try (InputStream ois = obj.getInputStream()) {
            IOUtils.copy(ois, System.out);
            System.out.println("Done");
        }
    }

    @Ignore
    @Test
    public void testS3AsyncBinaryProvider() throws IOException {
        assertNotNull(BUCKET);
        assertNotNull(REGION);
        ArrayList<String> keys = new ArrayList<>(List.of("1000-bytes"));
        S3AsyncBinaryProvider p = new S3AsyncBinaryProvider(50, 500, BUCKET, REGION, "/tmp", keys);
        BinaryObject obj = p.getBinary("IGNORED");
        System.out.println("FILE=" + ((LocalFileBinaryObject)obj).getAbsolutePath());
        try (InputStream ois = obj.getInputStream()) {
            IOUtils.copy(ois, System.out);
            System.out.println("Done");
        }
    }
}
