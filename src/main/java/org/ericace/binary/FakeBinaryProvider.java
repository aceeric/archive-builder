package org.ericace.binary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides binaries that don't come from anywhere or hold any data - but do provide an input stream of
 * contents. Simulates a binary from S3. The binary size can be randomized within a range, and the download
 * duration is derived from the size, based on a transfer rate defined by the {@link #BYTES_PER_SEC} field.
 */
public class FakeBinaryProvider implements BinaryProvider {

    private static final Logger logger = LogManager.getLogger(FakeBinaryProvider.class);

    /**
     * Transfer rate of approx. 1 megabyte/second based on testing in us-east-1
     */
    private static final float BYTES_PER_SEC = 1_000_000F;

    private final int minLength;
    private final int maxLength;

    /**
     * Constructor. Creates an instance based on passed size. The size determines the "download" time
     *
     * @param length The number of bytes for this Binary
     */
    public FakeBinaryProvider(int length) {
        this(length, length);
    }

    /**
     * Constructor. Creates an instance based on passed sizes. The size determines the "download" time
     *
     * @param minLength A min length. Actual instance length is randomized between this length and the
     *                  <code>macLength</code> parameter
     * @param maxLength Max length.
     */
    public FakeBinaryProvider(int minLength, int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
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
