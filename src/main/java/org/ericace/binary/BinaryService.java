package org.ericace.binary;

/**
 * A service that retrieves binary objects based on a configured provider.
 */
public class BinaryService {

    private final BinaryProvider provider;

    /**
     * Constructor
     *
     * @param provider The provider that will provide binary objects
     */
    public BinaryService(BinaryProvider provider) {
        this.provider = provider;
    }

    /**
     * Obtains a binary object corresponding to the passed key. Implementation is determined by the provider.
     *
     * @param key the key identifying the object.
     * @return the BinaryObject
     */
    public BinaryObject getBinary(String key) {
        return provider.getBinary(key);
    }

    /**
     * Performs a shutdown on the binary provider, as determined by the provider implementation
     */
    public void shutDownProvider() {
        provider.shutDownNow();
    }
}
