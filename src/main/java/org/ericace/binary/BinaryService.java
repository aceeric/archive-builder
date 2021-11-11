package org.ericace.binary;

import org.ericace.binary.BinaryObject;
import org.ericace.binary.BinaryProvider;

/**
 * A service that retrieves binary objects based on a provider.
 */
public class BinaryService {

    private final BinaryProvider provider;

    /**
     * Constructor
     *
     * @param provider The provider that will provide binaries
     */
    public BinaryService(BinaryProvider provider) {
        this.provider = provider;
    }

    /**
     * Obtains a binary object corresponding to the passed key. Implementation is determined by the provider.
     *
     * @param key the key identifying the object.
     *
     * @return the BinaryObject
     */
    public BinaryObject getBinary(long key) {
        return provider.getBinary(key);
    }
}
