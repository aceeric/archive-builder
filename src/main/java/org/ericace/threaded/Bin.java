package org.ericace.threaded;

import org.ericace.binary.BinaryObject;
import org.ericace.Document;

/**
 * Not "bin" as in "binary". "Bin" as in "container".
 * <p>
 * A value class that holds: a document, a binary object, and a sequence number representing the order in which
 * said document was consumed from a {@link org.ericace.DocumentReader}. This ordering is what enables binaries
 * to be gotten in parallel with varying completion times, but enables documents and their attachments to be
 * written to a TAR in original order.
 * <p>
 * Intended to be used as follows:
 * <ol>
 *     <li>A document processor reads a document from a document reader</li>
 *     <li>The document processor creates a <code>Bin</code> and places the document in the bin along with a
 *         monotonically increasing sequence number representing the order in which the document was read</li>
 *     <li>The bin is placed on a conveyor that goes to a pool of binary providers</li>
 *     <li>Each binary provider takes a bin from the conveyer as soon as it can, obtains the binary for the doc
 *         in the bin as quickly as it can, places that binary in the bin, and puts the bin on another conveyor
 *         to an ordering entity</li>
 *     <li>The ordering entity ensures that everything coming to it on the second conveyor is published to
 *         consumers in original order</li>
 *     <li>An archive builder reads from the ordering entity, and uses the doc and binary to create TAR entries</li>
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
