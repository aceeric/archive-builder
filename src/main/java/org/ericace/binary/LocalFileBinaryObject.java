package org.ericace.binary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A binary object over a local file.
 */
public class LocalFileBinaryObject implements BinaryObject {

    private static final Logger logger = LogManager.getLogger(LocalFileBinaryObject.class);

    /**
     * The physical file this binary wraps
     */
    private final File file;

    /**
     * Constructor
     *
     * @param file a file that this class wraps
     */
    LocalFileBinaryObject(File file) {
        this.file = file;
    }

    @Override
    public int getLength() {
        return (int) file.length();
    }

    /**
     * Returns an <code>InputStream</code> that deletes the underlying file on close
     *
     * @return the stream
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file) {
            @Override
            public void close() {
                file.delete();
            }
        };
    }

}
