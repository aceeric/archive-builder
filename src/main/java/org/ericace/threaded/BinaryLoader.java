package org.ericace.threaded;

import org.ericace.binary.BinaryService;
import org.ericace.Logger;
import org.ericace.binary.BinaryObject;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 
 * Gets binaries for documents. Reads from an incoming queue containing documents but no attachments. Gets
 * a binary attachment for a document, and places the document + attachment pair on an outgoing queue.
 */
public class BinaryLoader implements Runnable {

    /**
     * Provides {@link Bin} instances holding {@link org.ericace.Document} instances but no
     * {@link BinaryObject} instances.
     */
    private final BlockingQueue<Bin> incomingQueue;

    /**
     * Contains {@link Bin} instances into which a {@link BinaryObject} instance has been placed
     * by the class.
     */

    private final ReorderingQueue outgoingQueue;

    /**
     * The service that actually goes and gets a binary
     */
    private final BinaryService binaryService;

    /**
     * Enables clean shutdown
     */
    private boolean running = true;

    /**
     * Constructor
     *
     * @param incomingQueue see {@link #incomingQueue}
     * @param outgoingQueue see {@link #outgoingQueue}
     * @param binaryService see {@link #binaryService}
     */
    public BinaryLoader(BlockingQueue<Bin> incomingQueue, ReorderingQueue outgoingQueue, BinaryService binaryService) {
        this.incomingQueue = incomingQueue;
        this.outgoingQueue = outgoingQueue;
        this.binaryService = binaryService;
    }

    /**
     * Takes a {@link Bin} off the instance {@link #incomingQueue} that only has a <code>Document</code> in it. Gets
     * a {@link BinaryObject} representing the document's attachment via the instance {@link BinaryService}.
     * Puts the <code>BinaryObject</code> in the <code>Bin</code>, and puts the modified <code>Bin</code> into
     * the instance {@link ReorderingQueue}. Handles blocking and empty/full conditions on both incoming, and
     * outgoing queues via poll/sleep.
     */
    @Override
    public void run() {
        while (running) {
            Bin bin;
            try {
                if ((bin = incomingQueue.poll(100, TimeUnit.MILLISECONDS)) == null) {
                    Thread.sleep(100);
                } else {
                    bin.object = binaryService.getBinary(bin.doc.getKey());
                    while (!outgoingQueue.add(bin)) {
                        Logger.log(BinaryLoader.class, "Did not add: " + bin.doc.getName() + " - sleeping", true);
                        Thread.sleep(100);
                    }
                    Logger.log(BinaryLoader.class, "Added bin with binary to result queue: " + bin.doc.getName(), true);
                }
            } catch (InterruptedException e) {
                running = false;
            }
        }
    }
}
