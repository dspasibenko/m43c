package org.m43c.cc.atd;

public interface Offerer<T, P> {

    boolean offer(T t, P p);
    
}
