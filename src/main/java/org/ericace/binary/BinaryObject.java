package org.ericace.binary;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a binary document attachment whose store is separate from the document it is attached to. Once
 * it is brought down to the local system from the server, it might be cached on the local filesystem.
 */
public interface BinaryObject {
    /**
     * Gets the size of the Binary.
     *
     * @return size in bytes
     */
    public int getLength();

    /**
     * Gets an input stream over the binary contents.
     *
     * @return the stream
     */
    public InputStream getInputStream() throws IOException;
}
