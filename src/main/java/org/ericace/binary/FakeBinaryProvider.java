package org.ericace.binary;

/**
 * Provides binaries that don't come from anywhere or hold any data - but do provide an input stream of
 * contents. Simulates a binary from S3. The binary size can be randomized within a range, and the download
 * duratino is derived from the size, based on 1.4 megabytes per second (hard-coded.)
 */
public class FakeBinaryProvider implements BinaryProvider {

    /**
     * Transfer rate of 1.4 megabytes per second
     */
    private static final float MBS = 1_400_000F;

    private final int minLength;
    private final int maxLength;

    public FakeBinaryProvider(int length) {
        this(length, length);
    }

    public FakeBinaryProvider(int minLength, int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    /**
     * Calculate the size for this binary, sleep to simulate transfer time, then return a fake binary
     * with the calculated size.
     *
     * @param key Determines the content. See {@link FakeBinaryObject}
     *
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
        // min transfer time of 1/2 second
        float transferTime = Math.max(lengthActual / MBS, .5F);
        int sleepTime = (int)(transferTime * 1000);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was terminated");
        }
        return new FakeBinaryObject(key, (int)lengthActual);
    }
}
