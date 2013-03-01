package org.m43c.cc.atd;

import java.util.Comparator;

final class HolderComparator<T, P> implements Comparator<EntityHolder<T, P>> {
    
    private final Comparator<T> tComparator;
    
    HolderComparator(Comparator<T> tComparator) {
        this.tComparator = tComparator;
    }

    @Override
    public int compare(EntityHolder<T, P> o1, EntityHolder<T, P> o2) {
        return tComparator.compare(o1.getEntity(), o2.getEntity());
    }

}
