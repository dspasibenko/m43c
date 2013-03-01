package org.m43c.cc.atd;

import java.util.Comparator;
import java.util.TreeSet;

final class TwoSetQueue<P extends QueueEntity<T>, T extends QueueEntity<P>> {

    private final TreeSet<P> pSet;
    
    private final TreeSet<T> tSet;
    
    TwoSetQueue(Comparator<P> pComparator, Comparator<T> tComparator) {
        pSet = new TreeSet<P>(pComparator);
        tSet = new TreeSet<T>(tComparator);
    }
 
    boolean putT(T t) {
        if (!tryToOffer(t, pSet)) {
            tSet.add(t);
            return true;
        }
        return false;
    }
    
    boolean putP(P p) {
        if (!tryToOffer(p, tSet)) {
            pSet.add(p);
            return true;
        }
        return false;
    }
    
    boolean removeT(T t) {
        return tSet.remove(t);
    }
    
    boolean removeP(P p) {
        return pSet.remove(p);
    }
    
    void clear() {
        tSet.clear();
        pSet.clear();
    }
    
    private <A extends QueueEntity<B>, B extends QueueEntity<A>> boolean tryToOffer(A a, TreeSet<B> set) {
        B offerCandidate = findCandidate(a, set);
        if (offerCandidate != null) {
            offerCandidate.offer(a);
            return true;
        }
        return false;
    }
    
    private <A extends QueueEntity<B>, B extends QueueEntity<A>> B findCandidate(A a, TreeSet<B> set) {
        for (B b: set) {
            if (b.isAcceptableFor(a) && a.isAcceptableFor(b)) {
                return b;
            }
        }
        return null;
    }
   
    @Override
    public String toString() {
        return new StringBuilder().append("{pSet=").append(pSet).append(", tSet=")
                .append(tSet).append("}").toString();
    }
}
