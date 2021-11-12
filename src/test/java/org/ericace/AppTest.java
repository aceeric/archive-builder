package org.ericace;

import static org.junit.Assert.assertTrue;

import org.apache.commons.compress.utils.IOUtils;
import org.ericace.binary.BinaryObject;
import org.ericace.binary.S3BinaryProvider;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    // TODO get from JVM properties:
    
    private static final String BUCKET = "";
    private static final String REGION = "";
    @Test
    public void shouldAnswerWithTrue() throws IOException {
        S3BinaryProvider p = new S3BinaryProvider(BUCKET, REGION, "/tmp");
        BinaryObject obj = p.getBinary("1000bytes");
        try (InputStream ois = obj.getInputStream()) {
            IOUtils.copy(ois, System.out);
            System.out.println("Done");
        }
    }
}
