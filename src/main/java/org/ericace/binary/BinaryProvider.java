package org.ericace.binary;

/**
 * Provides {@link BinaryObject} instances.
 */
public interface BinaryProvider {
    /**
     * Gets a {@link BinaryObject} instance.
     *
     * @param key The key, like "foo", or maybe "foo/bar/baz/frobozz"
     * @return The object.
     */
    BinaryObject getBinary(String key);
}
