package org.m43c.cc.atd;

import java.util.Comparator;

public final class Queue<K, T, P> {

    private final K key;
    
    private final TwoSetQueue<EntityHolder<P, T>, EntityHolder<T, P>> queue;

    public Queue(K key, Comparator<T> tComp, Comparator<P> pComp) {
        this.key = key;
        this.queue = new TwoSetQueue<EntityHolder<P,T>, EntityHolder<T,P>>(
                new HolderComparator<P, T>(pComp), new HolderComparator<T, P>(tComp));
    }
    
    public K getKey() {
        return key;
    }
    
    TwoSetQueue<EntityHolder<P, T>, EntityHolder<T, P>> getQueue() {
        return queue;
    }
}
