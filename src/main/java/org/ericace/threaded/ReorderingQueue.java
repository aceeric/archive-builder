package org.ericace.threaded;

import org.ericace.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Accepts {@link Bin} instances <b>out of order</b> from multiple concurrent producers, and provides the instances
 * <b>in order</b> to a consumer (or consumers.)
 */
public class ReorderingQueue {
    private final ConcurrentHashMap<Long, Bin> map;
    final private AtomicLong nextSequence = new AtomicLong(1);
    final private AtomicLong totalItems = new AtomicLong(-1);
    final private AtomicLong itemsReturned = new AtomicLong(0);
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
        this.totalItems.set(totalItems);
    }

    /**
     * Gets a <code>Bin</code> instance <b>in order</b> from the internal cache and returns it to the caller, or
     * blocks if none are available. Order is determined by the <code>sequence</code> field of the <code>Bin</code>
     * class.
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
        String msg = String.format("Took bin for doc %s; next sequence=%d; items returned=%d; total items=%d",
                bin.doc.getName(), nextSeq, returned, totalItems.get());
        Logger.log(ReorderingQueue.class, msg);
        return bin;
    }

    /**
     * Adds a <code>Bin</code> instance to the internal cache. Entries can be added out of order. However,
     * once the internal cache is full, no more out-of-order entries are accepted.
     *
     * @param bin The instance to add
     * @return True if added, else false
     */
    public boolean add(Bin bin) {
        if (canAdd(bin)) {
            map.put(bin.sequence, bin);
            String msg = String.format("Added bin to queue: %s, queue size=%d", bin.doc.getName(), map.size());
            Logger.log(ReorderingQueue.class, msg);
            return true;
        } else {
            String msg = String.format("Can't add %s - bin sequence=%d, last sequence=%d, map size=%d",
                    bin.doc.getName(), bin.sequence, nextSequence.get(), map.size());
            Logger.log(ReorderingQueue.class, msg);
            return false;
        }
    }

    /**
     * Determines if a <code>Bin</code> instance can be added to the internal cache. The logic here is: if the
     * item being added is the next sequence that the class would return, accept it. Otherwise, accept anything
     * as long is the cache represented by the instance {@link #map} has capacity. Regarding that capacity,
     * always leave one space for that next sequential object in the order needed to preserve original order.
     *
     * @param bin the instance that the caller wants to add
     * @return true if it can be added per rules as described
     */
    private boolean canAdd(Bin bin) {
        return bin.sequence - 1 == nextSequence.get() || map.size() < capacity - 1;
    }
}
