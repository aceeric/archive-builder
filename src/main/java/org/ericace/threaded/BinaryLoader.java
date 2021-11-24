package org.ericace.threaded;

import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ericace.binary.BinaryObject;
import org.ericace.binary.BinaryService;

import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gets binaries for documents. Reads from an incoming queue containing documents but no attachments. Gets
 * a binary attachment for a document, and places the document + attachment pair on an outgoing queue.
 */
public class BinaryLoader implements Runnable {

    static final Summary downloadedBytes = Summary.build()
            .name("binary_bytes_downloaded").help("Binary attachment bytes downloaded.").register();
    static final Counter incomingQueueEmpty = Counter.build().name("binary_loader_incoming_queue_empty")
            .help("Count of times the binary loader did not have a binary to download").register();
    static final Counter outgoingQueueFull = Counter.build().name("binary_loader_outgoing_queue_full")
            .help("Count of times the binary loader blocked trying to offer binary to outgoing queue").register();
    private static final Logger logger = LogManager.getLogger(BinaryLoader.class);
    /**
     * Earliest start - provides most accurate representation of elapsed time for all threads, along with
     * {@link #latestFinish}
     */
    static AtomicLong earliestStart = new AtomicLong(Long.MAX_VALUE);

    /**
     * Latest finish, along with {@link #earliestStart}
     */
    static AtomicLong latestFinish = new AtomicLong(0);

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
     * A service that actually gets a binary
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
        logger.info("Started");
        Bin bin;
        while (running) {
            try {
                if ((bin = incomingQueue.poll(100, TimeUnit.MILLISECONDS)) == null) {
                    logger.info("Poll returned null - size = {}", incomingQueue.size());
                    incomingQueueEmpty.inc();
                    Thread.sleep(100);
                } else {
                    earliestStart.set(Math.min(Instant.now().toEpochMilli(), earliestStart.get()));
                    bin.object = binaryService.getBinary(bin.doc.getKey());
                    downloadedBytes.observe(bin.object.getLength());
                    latestFinish.set(Math.max(Instant.now().toEpochMilli(), latestFinish.get()));
                    while (!outgoingQueue.add(bin)) {
                        logger.info("Did not add: {} - sleeping", bin.doc.getName());
                        outgoingQueueFull.inc();
                        Thread.sleep(100);
                    }
                    logger.info("Added bin with binary to result queue: {}", bin.doc.getName());
                }
            } catch (InterruptedException e) {
                logger.info("Interrupted - stopping");
                running = false;
            }
        }
    }
}
