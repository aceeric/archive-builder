package org.ericace.binary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * A fake Binary that doesn't contain anything, but provides an input stream that returns content that
 * supports testing. The purpose of this class is to enable separation of measurement of threading and
 * concurrency from the specifics of real I/O against a binary store.
 */
public class FakeBinaryObject implements BinaryObject {

    private static final Logger logger = LogManager.getLogger(FakeBinaryObject.class);

    /**
     * The length of the binary
     */
    private final int length;

    /**
     * The binary key, represented as a string, stored in a byte array. E.g. ['1','0','0']
     */
    private final byte[] keyBytes;

    /**
     * Used by the InputStream instance returned by the {@link #getInputStream()} method
     */
    private int bytesRead = 0;

    /**
     * Used by the InputStream instance returned by the {@link #getInputStream()} method
     */
    private int idx = 0;

    /**
     * Constructor
     *
     * @param key    the object key - used by this implementation to generate the object content per
     *               the {@link #getInputStream()} method
     * @param length the length of this binary
     */
    public FakeBinaryObject(String key, int length) {
        this.length = length;
        keyBytes = key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public int getLength() {
        return length;
    }

    /**
     * Returns an input stream over faked binary contents as follows: Takes the object key, converts
     * it to a string, and returns one byte from the string round-robin on each read. E.g. if key = 100
     * then returns "100100100100100100100..." until {@link #length} bytes are read then returns -1 (EOF).
     */
    @Override
    public InputStream getInputStream() {
        return new InputStream() {
            @Override
            public int read() {
                if (bytesRead >= length) {
                    return -1;
                }
                byte b = keyBytes[idx++];
                if (idx >= keyBytes.length) {
                    idx = 0;
                }
                ++bytesRead;
                return b;
            }
        };
    }
}
