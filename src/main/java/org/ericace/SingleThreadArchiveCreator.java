package org.ericace;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ericace.binary.BinaryObject;
import org.ericace.binary.BinaryService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.time.Instant;
import java.util.zip.GZIPOutputStream;

/**
 * In a single thread, builds a TAR archive using a {@link DocumentReader} instance that provide documents,
 * and a {@link BinaryService} that provides each document's attachment. (Each doc is assumed to have only
 * one attachment.) The idea is: documents are stored in - and provided by - one store, and the attachments
 * are stored in - and provided by - a different store. For example, the documents might be stored in
 * ElasticSearch with relatively low read latency, and the attachments might be stored in an S3-compatible
 * service with higher latency. To include the attachments in the archive, it might be necessary to first
 * download them to a file on the local filesystem. The <code>DocumentReader</code> and <code>BinaryService</code>
 * classes encapsulate those storage details.
 */
public class SingleThreadArchiveCreator implements ArchiveCreator {

    private static final Logger logger = LogManager.getLogger(SingleThreadArchiveCreator.class);

    private final DocumentReader reader;
    private final BinaryService binaryService;
    private final String tarFQPN;
    private final Metrics metrics;

    /**
     * Constructor for builder
     */
    private SingleThreadArchiveCreator(SingleThreadArchiveCreator.Builder builder) {
        this.reader = builder.reader;
        this.binaryService = builder.binaryService;
        this.tarFQPN = builder.tarFQPN;
        this.metrics = builder.metrics;
    }

    /**
     * Creates a TAR in a single thread. The total amount of time will be throttled by the time required to
     * sequentially get each binary attachment.
     */
    @Override
    public void createArchive() {
        logger.info("Creating archive: {}", tarFQPN);
        try (GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(tarFQPN));
             ArchiveOutputStream aos = new TarArchiveOutputStream(gzos)) {
            for (Document doc : reader) {
                TarArchiveEntry entry = new TarArchiveEntry(doc.getName());
                BinaryObject obj = binaryService.getBinary(doc.getKey());
                entry.setSize(obj.getLength());
                entry.setModTime(Date.from(Instant.now()));
                aos.putArchiveEntry(entry);
                try (InputStream ois = obj.getInputStream()) {
                    IOUtils.copy(ois, aos);
                }
                aos.closeArchiveEntry();
                logger.info("Created entry for {}", doc.getName());
            }
            aos.finish();
            logger.info("Done creating archive");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Metrics getMetrics() {
        return metrics;
    }

    /**
     * Builder pattern
     */
    public static class Builder {
        private DocumentReader reader;
        private BinaryService binaryService;
        private String tarFQPN;
        private Metrics metrics;

        public SingleThreadArchiveCreator.Builder reader(DocumentReader reader) {
            this.reader = reader;
            return this;
        }

        public SingleThreadArchiveCreator.Builder binaryService(BinaryService binaryService) {
            this.binaryService = binaryService;
            return this;
        }

        public SingleThreadArchiveCreator.Builder tarFQPN(String tarFQPN) {
            this.tarFQPN = tarFQPN;
            return this;
        }

        public SingleThreadArchiveCreator.Builder metrics(Metrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public SingleThreadArchiveCreator build() {
            return new SingleThreadArchiveCreator(this);
        }
    }
}
