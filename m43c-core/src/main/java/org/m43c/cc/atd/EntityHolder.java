package org.m43c.cc.atd;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

final class EntityHolder<A, B> implements QueueEntity<EntityHolder<B, A>> {
    
    private final A entity;
    
    private final Collection<?> queues;
    
    private final Offerer<EntityHolder<A, B>, EntityHolder<B, A>> offerer;
    
    private Set<B> blackList;
    
    private boolean cancelled;
    
    EntityHolder(A entity, Collection<?> queues, Offerer<EntityHolder<A, B>, EntityHolder<B, A>> offerer) {
        this.entity = entity;
        this.queues = queues;
        this.offerer = offerer;
    }
    
    @Override
    public boolean isAcceptableFor(EntityHolder<B, A> entity) {
        return !cancelled && (blackList == null || !blackList.contains(entity.entity));
    }

    @Override
    public void offer(EntityHolder<B, A> entity) {
        if (offerer == null) {
            if (entity.offerer == null) {
                throw new AssertionError("Severe misconfiguration issue: at least one holder should contain not-null offerer.");
            }
            entity.offer(this);
            return;
        }
        offerer.offer(this, entity);
    }
    
    A getEntity() {
        return entity;
    }
    
    @SuppressWarnings("unchecked")
    <K> Collection<K> getQueues() {
        return (Collection<K>) queues;
    }
    
    void addToBlackList(B b) {
        if (blackList == null) {
            blackList = new HashSet<B>(1);
        }
        blackList.add(b);
    }
    
    void cancel() {
        cancelled = true;
    }

    @Override
    public String toString() {
        return "{entity=" + entity + ", cancelled=" + cancelled + ", blackListSize=" + blackList.size() +"}";
    }
}
