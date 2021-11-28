/**
 * The <code>threaded</code> package has the classes that implement the concurrent archive builder. The main
 * driver class is the <code>ThreadedArchiveCreator</code> class. That class spins up three independent
 * execution threads:
 * <ol>
 *     <li>One thread runs an EnqueuingDocumentReader, which provides documents into a queue.</li>
 *     <li>A thread pool runs multiple BinaryLoader instances. Each thread takes a Document from the
 *         queue above, gets a BinaryObject (from a BinaryService) representing the document
 *         attachment, and writes the Document + BinaryObject to a second queue. This second
 *         queue can be written to out-of-order (representing staggered completion times for
 *         S3 downloads) but presents the items to the queue consumer in the same order that
 *         was encountered by EnqueuingDocumentReader in the first thread. </li>
 *     <li>Finally, an InternalArchiveCreator instance is started on another thread that reads
 *         from the queue populated by the BinaryLoader thread pool and actually creates the
 *         tar gzip archive. Because of the ordering behavior of the queue, the archive is created
 *         in the same order as the order of documents read by the EnqueuingDocumentReader.</li>
 *     <li></li>
 * </ol>
 */
package org.ericace.threaded;