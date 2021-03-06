package org.ericace.threaded;

import org.ericace.Document;
import org.ericace.binary.BinaryObject;

/**
 * "Bin" as in "container", not "bin" as in "binary".
 * <p>
 * The <code>Bin</code> class is a value class that holds: a document, a binary object, and a sequence number
 * representing the order in which said document was consumed from a {@link org.ericace.DocumentReader}.
 * <p>
 * Intended to be used as follows:
 * <ol>
 *     <li>A document processor reads a document from a document reader</li>
 *     <li>The document processor creates a <code>Bin</code> and places the document in the bin along with a
 *         monotonically increasing sequence number representing the order in which the document was read
 *         from the reader</li>
 *     <li>The bin is placed on a conveyor that goes to a pool of binary providers</li>
 *     <li>Each binary provider takes a bin from the conveyor as soon as it can, obtains the binary for the doc
 *         in the bin as quickly as it can, places that binary in the bin, and puts the bin (out of order because
 *         of concurrency) on another conveyor to an ordering entity</li>
 *     <li>The ordering entity ensures that everything coming to it on the second conveyor out of order is
 *         published to consumers in original order</li>
 *     <li>An archive builder reads from the ordering entity, and uses the doc and binary to create TAR entries
 *         in the same order that documents were read in step one</li>
 * </ol>
 */
public class Bin {
    public Document doc;
    public BinaryObject object;
    public long sequence;

    public Bin(Document doc, long sequence) {
        this.doc = doc;
        this.sequence = sequence;
    }
}
