package org.ericace;

import java.util.Iterator;

/**
 * Provides a reader that returns {@link Document} instances to the caller. This is fake reader that
 * simply manufactures documents with 1-up IDs. This class simulates a document reader that reads some external
 * store (e.g. ElasticSearch) and presents the ElasticSearch results wrapped in a <i>Document</i> abstraction.
 */
public class DocumentReader implements Iterable<Document> {

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
     * The iterator returned by the {@link #iterator()} method
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
         * Creates a new document on each call. Each document creates its own unique ID. This will
         * also assign the sequence starting with one and monotonically increasing for each document.
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
