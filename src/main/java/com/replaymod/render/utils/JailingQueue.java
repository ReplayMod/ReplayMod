package com.replaymod.render.utils;

import com.google.common.base.Preconditions;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

//#if MC>=10904
import java.util.concurrent.PriorityBlockingQueue;
//#else
//$$ import java.util.AbstractQueue;
//#endif

public class JailingQueue<T>
        //#if MC>=10904
        extends PriorityBlockingQueue<T>
        //#else
        //$$ extends AbstractQueue<T> implements BlockingQueue<T>
        //#endif
{
    //#if MC>=10904
    private final PriorityBlockingQueue<T> delegate;
    //#else
    //$$ private final BlockingQueue<T> delegate;
    //#endif
    private final Set<Thread> jailed = new HashSet<>();

    //#if MC>=10904
    public JailingQueue(PriorityBlockingQueue<T> delegate) {
    //#else
    //$$ public JailingQueue(BlockingQueue<T> delegate) {
    //#endif
        this.delegate = delegate;
    }

    public synchronized void jail(int atLeast) {
        while (jailed.size() < atLeast) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
    }

    public synchronized void free(Thread thread) {
        Preconditions.checkState(jailed.remove(thread), "Thread is not jailed.");
        thread.interrupt();
    }

    public synchronized void freeAll() {
        jailed.clear();
        notifyAll();
    }

    private synchronized void tryAccess() {
        jailed.add(Thread.currentThread());
        notifyAll();
        while (jailed.contains(Thread.currentThread())) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
    }

    @Override
    public Iterator<T> iterator() {
        tryAccess();
        return delegate.iterator();
    }

    @Override
    public int size() {
        tryAccess();
        return delegate.size();
    }

    @Override
    public void put(T t)
            //#if MC<10904
            //$$ throws InterruptedException
            //#endif
    {
        tryAccess();
        delegate.put(t);
    }

    @Override
    public boolean offer(T t, long timeout, TimeUnit unit)
            //#if MC<10904
            //$$ throws InterruptedException
            //#endif
    {
        tryAccess();
        return delegate.offer(t, timeout, unit);
    }

    @Override
    public T take() throws InterruptedException {
        tryAccess();
        return delegate.take();
    }

    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        tryAccess();
        return delegate.poll(timeout, unit);
    }

    @Override
    public int remainingCapacity() {
        tryAccess();
        return delegate.remainingCapacity();
    }

    @Override
    public int drainTo(Collection<? super T> c) {
        tryAccess();
        return delegate.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        tryAccess();
        return delegate.drainTo(c, maxElements);
    }

    @Override
    public boolean offer(T t) {
        tryAccess();
        return delegate.offer(t);
    }

    @Override
    public T poll() {
        tryAccess();
        return delegate.poll();
    }

    @Override
    public T peek() {
        tryAccess();
        return delegate.peek();
    }
}
