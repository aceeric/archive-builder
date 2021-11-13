package org.ericace.binary;

/**
 * Provides {@link BinaryObject} instances.
 */
public interface BinaryProvider {
    /**
     * Gets a {@link BinaryObject} instance.
     *
     * @param key The key, like "foo", or maybe an MD5 sum.
     * @return The object.
     */
    BinaryObject getBinary(String key);
}
