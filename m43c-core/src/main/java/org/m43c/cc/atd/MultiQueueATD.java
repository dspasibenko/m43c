package org.m43c.cc.atd;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;

import org.jrivets.log.Logger;

import com.google.common.collect.ImmutableCollection;

public final class MultiQueueATD<K, P, T> {

    private final Map<T, EntityHolder<T, P>> tasks = new HashMap<T, EntityHolder<T, P>>();

    private final Map<P, EntityHolder<P, T>> processors = new HashMap<P, EntityHolder<P, T>>();

    private final Map<K, Queue<K, T, P>> queues = new HashMap<K, Queue<K, T, P>>();

    private final Logger logger;

    private final Lock lock;
    
    private final ExecutorService executor;

    private final Offerer<T, P> offerer;

    private final HoldersOfferer holdersOfferer = new HoldersOfferer();

    private class HoldersOfferer implements Offerer<EntityHolder<T, P>, EntityHolder<P, T>> {

        @Override
        public boolean offer(EntityHolder<T, P> tHolder, EntityHolder<P, T> pHolder) {
            removeTaskHolderFromQueues(tHolder);
            removeProcHolderFromQueues(pHolder);
            executor.execute(new OfferTask(tHolder, pHolder));
            return true;
        }
    }

    private class OfferTask implements Runnable {

        private final EntityHolder<T, P> tHolder;
        private final EntityHolder<P, T> pHolder;

        OfferTask(EntityHolder<T, P> tHolder, EntityHolder<P, T> pHolder) {
            this.tHolder = tHolder;
            this.pHolder = pHolder;
        }

        @Override
        public void run() {
            boolean offerResult = makeOffer();
            lock.lock();
            try {
                onOfferDone(offerResult);
            } finally {
                lock.unlock();
            }
        }

        private boolean makeOffer() {
            try {
                return offerer.offer(tHolder.getEntity(), pHolder.getEntity());
            } catch (Throwable t) {
                logger.error("Exception while offering ", tHolder, " to ", pHolder, t);
            }
            return false;
        }

        private void onOfferDone(boolean offerResult) {
            if (offerResult) {
                tasks.remove(tHolder.getEntity());
                processors.remove(pHolder.getEntity());
                return;
            }
            tHolder.addToBlackList(pHolder.getEntity());
            putTaskHolderToQueuesSafely(tHolder);
            putProcHolderToQueuesSafely(pHolder);
        }

    }

    public MultiQueueATD(Logger logger, Lock lock, ExecutorService executor, Offerer<T, P> offerer) {
        this.logger = logger;
        this.lock = lock;
        this.executor = executor;
        this.offerer = offerer;
    }

    public void distributeTask(T t) {
        distributeTask(t, null);
    }

    public void distributeTask(T t, ImmutableCollection<K> queues) {
        lock.lock();
        try {
            if (tasks.containsKey(t)) {
                throw new IllegalArgumentException("Cannot register the same task twice.");
            }
            registerTaskInternal(t, queues);
        } finally {
            lock.unlock();
        }
    }

    public boolean cancelTask(T t) {
        lock.lock();
        try {
            return cancelTaskInternal(t);
        } finally {
            lock.unlock();
        }
    }

    public void registerProc(P p) {
        registerProc(p, null);
    }

    public void registerProc(P p, ImmutableCollection<K> queues) {
        lock.lock();
        try {
            if (processors.containsKey(p)) {
                throw new IllegalArgumentException("Cannot register the same processor twice.");
            }
            registerProcInternal(p, queues);
        } finally {
            lock.unlock();
        }
    }

    public boolean unregisterProc(P p) {
        lock.lock();
        try {
            return cancelProcInternal(p);
        } finally {
            lock.unlock();
        }
    }
    
    public void addQueue(Queue<K, T, P> queue) {
        lock.lock();
        try {
            if (queues.containsKey(queue.getKey())) {
                throw new IllegalStateException("The queue with " + queue.getKey() + " is already registered in the ATD.");
            }
            queues.put(queue.getKey(), queue);
        } finally {
            lock.unlock();
        }
    }
    
    public boolean removeQueue(K key) {
        lock.lock();
        try {
            Queue<K, T, P> queue = queues.remove(key);
            if (queue != null) {
                queue.getQueue().clear();
            }
            return queue != null;
        } finally {
            lock.unlock();
        }
    }

    private void registerTaskInternal(T t, ImmutableCollection<K> queues) {
        EntityHolder<T, P> holder = new EntityHolder<T, P>(t, queues, holdersOfferer);
        tasks.put(t, holder);
        putTaskHolderToQueuesSafely(holder);
    }

    private boolean putTaskHolderToQueuesSafely(EntityHolder<T, P> holder) {
        try {
            return putTaskHolderToQueues(holder);
        } catch (RuntimeException re) {
            cancelTaskInternal(holder.getEntity());
            throw re;
        }
    }

    private boolean cancelTaskInternal(T t) {
        EntityHolder<T, P> holder = tasks.remove(t);
        if (holder != null) {
            holder.cancel();
            removeTaskHolderFromQueues(holder);
            return true;
        }
        return false;
    }

    private void registerProcInternal(P p, ImmutableCollection<K> queues) {
        EntityHolder<P, T> holder = new EntityHolder<P, T>(p, queues, null);
        processors.put(p, holder);
        putProcHolderToQueuesSafely(holder);
    }

    private boolean putProcHolderToQueuesSafely(EntityHolder<P, T> holder) {
        try {
            return putProcHolderToQueues(holder);
        } catch (RuntimeException re) {
            cancelProcInternal(holder.getEntity());
            throw re;
        }
    }

    private boolean cancelProcInternal(P p) {
        EntityHolder<P, T> holder = processors.remove(p);
        if (holder != null) {
            holder.cancel();
            removeProcHolderFromQueues(holder);
            return true;
        }
        return false;
    }

    private boolean putTaskHolderToQueues(EntityHolder<T, P> holder) {
        Collection<K> distQueues = holder.getQueues();
        if (distQueues == null) {
            distQueues = queues.keySet();
        }
        for (K key: distQueues) {
            Queue<K, T, P> queue = queues.get(key);
            if (!queue.getQueue().putT(holder)) {
                return false;
            }
        }
        return true;
    }

    private boolean putProcHolderToQueues(EntityHolder<P, T> holder) {
        Collection<K> distQueues = holder.getQueues();
        if (distQueues == null) {
            distQueues = queues.keySet();
        }
        for (K key: distQueues) {
            Queue<K, T, P> queue = queues.get(key);
            if (!queue.getQueue().putP(holder)) {
                return false;
            }
        }
        return true;
    }

    private void removeTaskHolderFromQueues(EntityHolder<T, P> holder) {
        for (Queue<K, T, P> queue : queues.values()) {
            queue.getQueue().removeT(holder);
        }
    }

    private void removeProcHolderFromQueues(EntityHolder<P, T> holder) {
        for (Queue<K, T, P> queue: queues.values()) {
            queue.getQueue().removeP(holder);
        }
    }
}
