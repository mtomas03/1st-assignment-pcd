package pcd.poool.util;

import java.util.LinkedList;

public class BoundedBuffer<T> {

    private final LinkedList<T> buffer = new LinkedList<>();
    private final int maxSize;

    public BoundedBuffer(int maxSize) {
        this.maxSize = maxSize;
    }

    public synchronized void put(T item) throws InterruptedException {
        while (buffer.size() == maxSize) wait();
        buffer.addLast(item);
        notifyAll();
    }

    public synchronized T get() throws InterruptedException {
        while (buffer.isEmpty()) wait();
        T item = buffer.removeFirst();
        notifyAll();
        return item;
    }

    /**
     * Non-blocking: returns null if the buffer is currently empty.
     */
    public synchronized T poll() {
        if (buffer.isEmpty()) return null;
        T item = buffer.removeFirst();
        notifyAll();
        return item;
    }

    public synchronized boolean isEmpty() {
        return buffer.isEmpty();
    }
}
