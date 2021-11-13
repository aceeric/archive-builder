package org.ericace.threaded;

import org.ericace.Document;
import org.ericace.DocumentReader;
import org.ericace.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

/**
 * Reads {@link Document} instances from a {@link DocumentReader} and enqueues them for another thread to
 * get each document's binary attachment.
 */
public class EnqueuingDocumentReader implements Callable<Long> {

    /**
     * Provides documents from some external store
     */
    private final DocumentReader reader;

    /**
     * Contains sequenced <code>Bin</code> instances that another thread can obtain binaries for. The class
     * enqueues onto this queue from the {@link #reader}
     */
    private final BlockingQueue<Bin> binQueue;

    /**
     * Running count of documents read from the instance {@link #reader}. Also used as the sequencer.
     */
    private long documentCount = 0;

    /**
     * Constructor
     *
     * @param reader   see {@link #reader}
     * @param binQueue see {@link #binQueue}
     */
    public EnqueuingDocumentReader(DocumentReader reader, BlockingQueue<Bin> binQueue) {
        this.reader = reader;
        this.binQueue = binQueue;
    }

    /**
     * Reads {@link Document} instances from the instance {@link #reader}. For each document, creates a {@link Bin}
     * and places the document in the bin, along with a sequence number representing the order in which the doc
     * was read. Then places the <code>Bin</code> into the instance {@link #binQueue}. Handles the queue being
     * full by offer/sleep.
     *
     * @return the count of documents that were read
     */
    @Override
    public Long call() {
        for (Document doc : reader) {
            ++documentCount;
            Bin bin = new Bin(doc, documentCount);
            while (!binQueue.offer(bin)) {
                Logger.log(EnqueuingDocumentReader.class, "Document queue full at: " + doc.getName());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Logger.log(EnqueuingDocumentReader.class, String.format("Interrupted: read %d documents from reader",
                            documentCount));
                    return documentCount;
                }
            }
            Logger.log(EnqueuingDocumentReader.class, "Added doc to doc queue: " + doc.getName());
        }
        Logger.log(EnqueuingDocumentReader.class, String.format("Done: read %d documents from reader", documentCount));
        return documentCount;
    }
}
