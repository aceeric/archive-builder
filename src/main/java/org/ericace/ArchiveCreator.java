package org.ericace;

/**
 * Defines the interface for creating a TAR. The implementation class is expected to be initialized
 * by a builder, and so instance state will contain everything needed to generate the archive. Hence this
 * is a lean API.
 */
public interface ArchiveCreator {
    /**
     * Creates the archive. (Always creates a Gzip TAR even if you don't name it such, so you should name it
     * ...tar.gz
     */
    void createArchive();

    /**
     * Gets the metrics instance wired into the archive creator
     */
    Metrics getMetrics();
}
