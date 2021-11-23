package org.ericace;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.compress.utils.IOUtils;
import org.ericace.binary.BinaryObject;
import org.ericace.binary.S3BinaryProvider;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    private static final String BUCKET_PROPERTY = "bucket";
    private static final String REGION_PROPERTY = "region";

    private static final String BUCKET = System.getProperty(BUCKET_PROPERTY);
    private static final String REGION = System.getProperty(REGION_PROPERTY);

    /**
     * This isn't a test, it's for debugging S3 downloads
     */
    @Test
    public void testS3Download() throws IOException {
        assertNotNull(BUCKET);
        assertNotNull(REGION);
        ArrayList<String> keys = new ArrayList<>(List.of("1000-bytes"));
        S3BinaryProvider p = new S3BinaryProvider(BUCKET, REGION, "/tmp", keys);
        BinaryObject obj = p.getBinary("IGNORED");
        try (InputStream ois = obj.getInputStream()) {
            IOUtils.copy(ois, System.out);
            System.out.println("Done");
        }
    }
}
