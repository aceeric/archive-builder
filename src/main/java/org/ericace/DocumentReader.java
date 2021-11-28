package org.ericace;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;

/**
 * Provides a reader that returns {@link Document} instances to the caller. This is fake reader that
 * simply manufactures documents with 1-up IDs. This class simulates an actual document reader that reads some
 * external store (e.g. ElasticSearch) and presents its results wrapped in a <i>Document</i> abstraction.
 */
public class DocumentReader implements Iterable<Document> {

    private static final Logger logger = LogManager.getLogger(DocumentReader.class);

    /**
     * The number of docs this reader will return
     */
    private final int numDocs;

    /**
     * Constructor
     *
     * @param numDocs the number of docs this reader will return
     */
    public DocumentReader(int numDocs) {
        this.numDocs = numDocs;
    }

    @Override
    public Iterator<Document> iterator() {
        return new DocumentIterator(numDocs);
    }

    /**
     * The iterator returned by the {@link #iterator()} method. The iterator just manufactures documents
     * with a 1-up unique ID. See {@link #next()}.
     */
    private static class DocumentIterator implements Iterator<Document> {

        private final int numDocs;
        private int curDoc = 0;

        DocumentIterator(int numDocs) {
            this.numDocs = numDocs;
        }

        @Override
        public boolean hasNext() {
            return curDoc < numDocs;
        }

        /**
         * Creates a new document on each call. Each document creates its own unique ID starting with '1' and
         * monotonically increasing for each subsequent document.
         */
        @Override
        public Document next() {
            if (!hasNext()) {
                throw new IllegalStateException();
            }
            ++curDoc;
            Document doc = new Document();
            return doc;
        }
    }
}
