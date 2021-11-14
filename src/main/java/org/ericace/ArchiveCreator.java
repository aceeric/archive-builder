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
public class ArchiveCreator {

    private static final Logger logger = LogManager.getLogger(ArchiveCreator.class);

    /**
     * Creates a TAR in a single thread. The total amount of time will be throttled by the time required to
     * sequentially get each binary attachment.
     *
     * @param reader  A document reader that provides {@link Document} instances representing documents from
     *                some store
     * @param svc     Provides an interface to get binary attachments for a document from a store that is separate
     *                from the document store
     * @param tarFQPN The fully-qualified path of the archive to create/replace
     */
    static void createArchive(DocumentReader reader, BinaryService svc, String tarFQPN) throws IOException {
        logger.info("Creating archive: {}", tarFQPN);
        try (GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(tarFQPN));
             ArchiveOutputStream aos = new TarArchiveOutputStream(gzos)) {
            for (Document doc : reader) {
                TarArchiveEntry entry = new TarArchiveEntry(doc.getName());
                BinaryObject obj = svc.getBinary(doc.getKey());
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
        }
    }
}
