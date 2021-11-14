package org.ericace.threaded;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Accepts {@link Bin} instances <b>out of order</b> from multiple concurrent producers via the {@link #add(Bin)}
 * method, and provides the instances <b>in order</b> to a consumer (or consumers.) via the {@link #take()}
 * method.
 */
public class ReorderingQueue {

    private static final Logger logger = LogManager.getLogger(ReorderingQueue.class);

    /**
     * <code>Bin</code> instances are cached here
     */
    private final ConcurrentHashMap<Long, Bin> map;

    /**
     * Enables the class <code>take</code> ordering behavior. This value represents the next sequence value of the
     * <code>Bin</code> instance from the internal cache that will be returned via the <code>take</code> method.
     */
    final private AtomicLong nextSequence = new AtomicLong(1);

    /**
     * Total items to return to consumers. If -1 (set by class initializer) then we don't yet know how many items
     * to return so the <code>take</code> method can never return EOF. Once set, however, the <code>take</code> method
     * can use it to determine whether all items have been taken by consumers, and can thus retrurn EOF.
     */
    final private AtomicLong totalItems = new AtomicLong(-1);

    /**
     * Running count of items provided from the internal cache to consumers.
     */
    final private AtomicLong itemsReturned = new AtomicLong(0);

    /**
     * Internal cache size
     */
    final private int capacity;

    /**
     * Constructor
     *
     * @param capacity the number of items that the internal cache will hold before rejecting adds
     *                 by producers
     */
    public ReorderingQueue(int capacity) {
        this.capacity = capacity;
        map = new ConcurrentHashMap<>(capacity);
    }

    /**
     * Sets the total items that should be returned, enabling the class to return EOF. This number may not be
     * known when the class starts up. So the design allows this value to be set concurrently while the class
     * is running in a thread. Once the total items returned is >= this value, a {@link #take()} method call
     * will return EOF.
     *
     * @param totalItems The total items that should be returned
     */
    public void setTotalItems(long totalItems) {
        logger.info("Setting total items: {}", totalItems);
        this.totalItems.set(totalItems);
    }

    /**
     * Gets a <code>Bin</code> instance <b>in order</b> from the internal cache and returns it to the caller, or
     * blocks if no instances are available in order. Order is determined by the <code>sequence</code> field of
     * the <code>Bin</code> class.
     *
     * @return the next <code>Bin instance</code>
     * @throws InterruptedException per <code>Thread.sleep</code>
     */
    public Bin take() throws InterruptedException {
        while (!map.containsKey(nextSequence.get())) {
            Thread.sleep(100);
            if (totalItems.get() >= 0 && itemsReturned.get() >= totalItems.get()) {
                return null;
            }
        }
        Bin bin = map.get(nextSequence.get());
        map.remove(nextSequence.get());
        long nextSeq;
        long returned;
        synchronized (this) {
            nextSeq = nextSequence.incrementAndGet();
            returned = itemsReturned.incrementAndGet();
        }
        logger.info("Took bin for doc {}; next sequence={}; items returned={}; total items={}", bin.doc.getName(),
                nextSeq, returned, totalItems.get());
        return bin;
    }

    /**
     * Adds a <code>Bin</code> instance to the internal cache. Entries can be added out of order. Once
     * the internal cache is full, no more entries are accepted.
     *
     * @param bin The instance to add
     * @return True if added, else false
     */
    public boolean add(Bin bin) {
        if (canAdd(bin)) {
            map.put(bin.sequence, bin);
            logger.info("Added bin to queue: {}, queue size={}", bin.doc.getName(), map.size());
            return true;
        } else {
            logger.info("Can't add {} - bin sequence={}, last sequence={}, map size={}", bin.doc.getName(),
                    bin.sequence, nextSequence.get(), map.size());
            return false;
        }
    }

    /**
     * Determines if a <code>Bin</code> instance can be added to the internal cache. The logic is: if the
     * item being added is the next sequence that the class would return, accept it. Otherwise, accept anything
     * as long is the cache represented by the instance {@link #map} has capacity. Regarding that capacity,
     * always leave one space for that next sequential object in the order needed to preserve original order,
     * so the internal cache can't ever become full and prevent the next required ordered item to be added.
     *
     * @param bin the instance that the caller wants to add
     * @return true if it can be added per rules as described
     */
    private boolean canAdd(Bin bin) {
        return bin.sequence - 1 == nextSequence.get() || map.size() < capacity - 1;
    }
}
