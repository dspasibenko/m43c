package org.m43c.cc.atd;

interface QueueEntity<T> {

    boolean isAcceptableFor(T entity);
    
    void offer(T entity);
}
