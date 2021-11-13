package org.ericace;

/**
 * Represents a document. This is a simplified abstraction that provides a name, and a key representing an attachment.
 * The attachment is assumed to be stored in some other store, separately from the store that houses this document.
 */
public class Document {

    /**
     * Ensures every document created will have a unique key
     */
    private static long oneUp = 1;

    /**
     * This document's ID
     */
    private final long docID;

    /**
     * Constructor - initializes the {@link #docID} member with a unique 1-up number
     */
    public Document() {
        docID = oneUp++;
    }

    /**
     * Constructor with a specific ID, rather than the default 1-up behavior
     *
     * @param id the id
     */
    public Document(long id) {
        docID = id;
    }

    /**
     * Gets the document name, which is "file-" with the instance {@link #docID} appended. E.g. "file-100"
     */
    public String getName() {
        return "file-" + docID;
    }

    /**
     * Gets a key that represents a binary attachment to the document. In this case, the key is just
     * the document ID.
     */
    public String getKey() {
        return String.valueOf(docID);
    }
}
