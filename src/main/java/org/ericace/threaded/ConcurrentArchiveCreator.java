package org.ericace.threaded;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ericace.ArchiveCreator;
import org.ericace.DocumentReader;
import org.ericace.Metrics;
import org.ericace.binary.BinaryService;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Date;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.zip.GZIPOutputStream;

/**
 * Builds a TAR by concurrently downloading attachments using the passed {@link BinaryService} in parallel via a thread
 * pool. Otherwise, same as the {@link ArchiveCreator} class.
 */
public class ConcurrentArchiveCreator {

    private static final Logger logger = LogManager.getLogger(ConcurrentArchiveCreator.class);

    /**
     * This queue contains {@link Bin} instances, each holding both a document and a binary, and it is guaranteed
     * to provide items in the same order as the docs provided by the {@link DocumentReader} passed to
     * the {@link #createArchive} method.
     */
    private final ReorderingQueue archiveBuilderQueue;

    /**
     * This queue contains {@link Bin} instances with only a document. The pool of {@link BinaryLoader} instances
     * will read from this queue, get the binary for the doc, put it in the bin, and write the bin to
     * the {@link #archiveBuilderQueue}.
     */
    private final BlockingQueue<Bin> binaryLoaderQueue;

    /**
     * Runs 'n' threads started by the class: Some number of threads populate the {@link #archiveBuilderQueue}
     * per the {@link #binaryLoaderThreads} field, one thread populates the {@link #binaryLoaderQueue}, and
     * one thread consumes the <code>archiveBuilderQueue</code> to generate the TAR on the filesystem.
     */
    private final ThreadPoolExecutor executor;

    /**
     * The count of threads to concurrently get binaries
     */
    private final int binaryLoaderThreads;

    /**
     * The count of items to cache in memory in the two queues used by the class: the queue that feeds the
     * binary loader pool, and the queue that feeds the archive builder
     */
    private final int memCacheSize;

    /**
     * The reader that provides documents and metadata
     */
    private final DocumentReader reader;

    /**
     * The service that provides binary attachments for documents
     */
    private final BinaryService binaryService;

    /**
     * The fully qualified pathname of the archive to generate
     */
    private final String tarFQPN;

    /**
     * Metrics accumulation
     */
    private final Metrics metrics;

    /**
     * Constructor for builder
     */
    private ConcurrentArchiveCreator(Builder builder) {
        this.binaryLoaderThreads = builder.binaryLoaderThreads;
        this.memCacheSize = builder.memCacheSize;
        this.reader = builder.reader;
        this.binaryService = builder.binaryService;
        this.tarFQPN = builder.tarFQPN;
        this.metrics = builder.metrics;

        archiveBuilderQueue = new ReorderingQueue(memCacheSize);
        binaryLoaderQueue = new ArrayBlockingQueue<>(memCacheSize);

        // +2 because this pool is used for the binary downloaders as well as the document reader thread
        // and the archive creator thread
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(binaryLoaderThreads + 2);
    }

    /**
     * Creates an archive with concurrent downloads of document binaries based on class configuration
     * provided by the builder.
     *
     * @return True if success, else false
     */
    public boolean createArchive() throws InterruptedException, ExecutionException {
        logger.info("Creating archive: {}", tarFQPN);

        for (int i = 0; i < binaryLoaderThreads; ++i) {
            // populate a pool to download binaries from S3
            executor.submit(new BinaryLoader(binaryLoaderQueue, archiveBuilderQueue, binaryService));
        }

        // this future lets us know when all documents have been read from the reader and enqueued for
        // the pool of binary loaders
        Future<Long> documentCount = executor.submit(new EnqueuingDocumentReader(reader, binaryLoaderQueue));

        // The archive will be created on this thread
        Future<Boolean> archiveResult = executor.submit(new InternalArchiveCreator(archiveBuilderQueue, tarFQPN, metrics));

        // When 'documentCount.get()' returns, all documents have been read from the reader and enqueued for
        // the binary loader thread pool. Using this value to set the total items on the archive builder
        // queue allows that class to return EOF when it has provided the corresponding number of documents
        archiveBuilderQueue.setTotalItems(documentCount.get());

        // When 'archiveResult.get()' returns, the archive is generated
        boolean result = archiveResult.get();

        logger.info("Shutting down executor service and all associated threads");
        executor.shutdownNow();
        logger.info("Done");
        return result;
    }

    /**
     * Builder pattern
     */
    public static class Builder {
        private int binaryLoaderThreads;
        private int memCacheSize;
        private DocumentReader reader;
        private BinaryService binaryService;
        private String tarFQPN;
        private Metrics metrics;

        public Builder binaryLoaderThreads(int binaryLoaderThreads) {
            this.binaryLoaderThreads = binaryLoaderThreads;
            return this;
        }

        public Builder memCacheSize(int memCacheSize) {
            this.memCacheSize = memCacheSize;
            return this;
        }

        public Builder reader(DocumentReader reader) {
            this.reader = reader;
            return this;
        }

        public Builder binaryService(BinaryService binaryService) {
            this.binaryService = binaryService;
            return this;
        }

        public Builder tarFQPN(String tarFQPN) {
            this.tarFQPN = tarFQPN;
            return this;
        }

        public Builder metrics(Metrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public ConcurrentArchiveCreator build() {
            return new ConcurrentArchiveCreator(this);
        }
    }

    /**
     * Actually builds the archive on a thread, consuming the instance {@link #queue} which provides {@link Bin}
     * instances, each containing a {@link org.ericace.Document} instance - and its attachment represented
     * by a {@link org.ericace.binary.BinaryObject} instance.
     */
    public static class InternalArchiveCreator implements Callable<Boolean> {

        /**
         * A concurrent queue that provides {@link Bin} instances, each containing a document, and its binary
         * attachment. The order is guaranteed to be identical to the order presented to the parent class via its
         * {@link DocumentReader} instance.
         */
        private final ReorderingQueue queue;

        /**
         * The fully-qualified path name of the archive to generate
         */
        private final String tarFQPN;

        /**
         * Basic metrics
         */
        private final Metrics metrics;

        /**
         * Constructor
         *
         * @param queue   See {@link #queue}
         * @param tarFQPN See {@link #tarFQPN}
         * @param metrics See {@link #metrics}
         */
        InternalArchiveCreator(ReorderingQueue queue, String tarFQPN, Metrics metrics) {
            this.queue = queue;
            this.tarFQPN = tarFQPN;
            this.metrics = metrics;
        }

        /**
         * Creates the TAR file on the filesystem. Consumes the instance queue, which provides {@link Bin}
         * instances ordered in the same order presented to the parent class via its {@link DocumentReader} instance.
         * Also guaranteed by the internal queue: each item will contain both a document with metadata, and
         * a binary attachment.
         *
         * @return True if success, else False
         */
        @Override
        public Boolean call() throws Exception {
            try (GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(tarFQPN));
                 ArchiveOutputStream aos = new TarArchiveOutputStream(gzos)) {
                while (true) {
                    logger.info("Taking from the queue");
                    Bin bin = queue.take(); // blocks or returns EOF (null)
                    if (bin == null) {
                        logger.info("No more items - stopping");
                        break;
                    }
                    logger.info("Creating entry for {}", bin.doc.getName());
                    TarArchiveEntry entry = new TarArchiveEntry(bin.doc.getName());
                    entry.setSize(bin.object.getLength());
                    if (metrics != null) {
                        metrics.addBinaryBytesWritten(bin.object.getLength());
                    }
                    entry.setModTime(Date.from(Instant.now()));
                    aos.putArchiveEntry(entry);
                    try (InputStream ois = bin.object.getInputStream()) {
                        IOUtils.copy(ois, aos);
                    }
                    aos.closeArchiveEntry();
                    logger.info("Done creating entry");
                }
                aos.finish();
                logger.info("Done creating archive");
            } catch (Exception e) {
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }
    }
}
