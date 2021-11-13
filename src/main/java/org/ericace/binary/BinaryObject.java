package org.ericace.binary;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a binary document attachment whose store is separate from the document to which it is attached.
 */
public interface BinaryObject {
    /**
     * Gets the size of the Binary.
     *
     * @return size in bytes
     */
    int getLength();

    /**
     * Gets an input stream over the binary contents.
     *
     * @return the stream
     */
    InputStream getInputStream() throws IOException;
}
