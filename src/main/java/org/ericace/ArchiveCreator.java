package org.ericace;

/**
 * Defines the interface for a class that can create a TAR. The class is expected to be initialized with
 * a builder, and so instance state will contain everything needed to gen the archive.
 */
public interface ArchiveCreator {
    /**
     * Creates the archive. (Always creates a Gzip TAR even if you don't name it such, so you should name it
     * ...tar.gz
     */
    void createArchive();

    /**
     * Gets the metrics instance wired into the creator
     */
    Metrics getMetrics();
}
