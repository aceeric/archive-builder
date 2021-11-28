package org.ericace.binary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Provides binaries that don't come from anywhere or hold any data - but do provide an input stream of
 * contents. Simulates a binary from S3. The binary size can be randomized within a size range, and the download
 * duration simulated by the class is derived from the size, based on a transfer rate defined by
 * the {@link #BYTES_PER_SEC} field.
 */
public class FakeBinaryProvider implements BinaryProvider {

    private static final Logger logger = LogManager.getLogger(FakeBinaryProvider.class);

    /**
     * Transfer rate of approx. 1 megabyte/second based on testing in AWS us-east-1
     */
    private static final float BYTES_PER_SEC = 1_000_000F;

    private final int minLength;
    private final int maxLength;

    /**
     * Constructor. Creates an instance based on passed size. The size determines the "download" time
     *
     * @param lengths Either a single length, or two lengths: min,max
     */
    public FakeBinaryProvider(List<Integer> lengths) {
        minLength = lengths.get(0);
        maxLength = lengths.size() > 1 ? lengths.get(1) : minLength;
    }

    /**
     * Calculates the size for this binary, sleeps to simulate transfer time, then returns a fake binary
     * with the (randomized) calculated size.
     *
     * @param key Determines the content. See {@link FakeBinaryObject}
     * @return the BinaryObject
     */
    @Override
    public BinaryObject getBinary(String key) {
        float lengthActual;
        if (minLength == maxLength) {
            lengthActual = minLength;
        } else {
            lengthActual = (int) ((Math.random() * (maxLength - minLength)) + minLength);
        }
        // min transfer time of 1/2 second - is this accurate for AWS?
        float transferTime = Math.max(lengthActual / BYTES_PER_SEC, .5F);
        int sleepTime = (int) (transferTime * 1000);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was terminated");
        }
        return new FakeBinaryObject(key, (int) lengthActual);
    }
}
