package org.ericace.threaded;

import org.ericace.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Accepts {@link Bin} instances out of order from multiple concurrent producers, and provides the instances
 * in order to a consumer (or consumers.)
 */
public class ReorderingQueue {
    private final ConcurrentHashMap<Long, Bin> map;
    private AtomicLong nextSequence = new AtomicLong(1);
    private AtomicLong totalItems = new AtomicLong(-1);
    private AtomicLong itemsReturned = new AtomicLong(0);
    private int capacity;

    public ReorderingQueue(int capacity) {
        this.capacity = capacity;
        map = new ConcurrentHashMap<>(capacity);
    }

    public void setTotalItems(long totalItems) {
        this.totalItems.set(totalItems);
    }

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

    public boolean add(Bin bin) {
        if (canAdd(bin)) {
            map.put(bin.sequence, bin);
            long myLastSequence = 0;
            String msg = String.format("Added bin to queue: %s, my last sequence=%d, queue size=%d", bin.doc.getName(),
                    myLastSequence, map.size());
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
     * The logic here is - if the item being added is the next sequence that the class would return, accept it.
     * Otherwise accept anything as long is the instance {@link #map} has capacity. Regarding that capacity,
     * always leave one space for that next sequential object.
     *
     * @param bin the instance that the caller wants to add
     *
     * @return true if it can be added per rules as described
     */
    private boolean canAdd(Bin bin) {
        return bin.sequence - 1 == nextSequence.get() || map.size() < capacity - 1;
    }
}
