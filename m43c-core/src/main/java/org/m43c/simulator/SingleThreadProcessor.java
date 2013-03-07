package org.m43c.simulator;

import java.util.Comparator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jrivets.collection.SortedArray;

public final class SingleThreadProcessor implements Processor {

    private final Lock lock = new ReentrantLock();

    private Condition resumeCondition;

    private long runningThreadId;

    private ProcessorState state = ProcessorState.HALTED;

    private final Quartz quartz = new Quartz();

    private final SortedArray<ProcessorWrapper> execQueue = new SortedArray<ProcessorWrapper>(
            new ProcessorWrapperComparator(), 10);

    private static class ProcessorWrapperComparator implements Comparator<ProcessorWrapper> {
        @Override
        public int compare(ProcessorWrapper o1, ProcessorWrapper o2) {
            return (int) (o1.nextExecTime - o2.nextExecTime);
        }
    }

    private static class ProcessorWrapper {

        private long nextExecTime;

        private final Process process;

        public ProcessorWrapper(long nextExecTime, Process process) {
            this.nextExecTime = nextExecTime;
            this.process = process;
        }
    }

    // -------------------------------------------------------------------------
    // Processor interface
    // -------------------------------------------------------------------------
    @Override
    public void schedule(Process process, long delayMs) {
        assertInvokerThreadId();
        if (process == null) {
            throw new NullPointerException();
        }
        execQueue.add(new ProcessorWrapper(quartz.currentTimeMillis() + delayMs, process));
    }

    @Override
    public void run(Process process) throws InterruptedException {
        lock.lock();
        try {
            if (runningThreadId != 0L) {
                throw new AssertionError("Illegal usage of the single-thread processor or severe bug.");
            }
            runningThreadId = Thread.currentThread().getId();
            state = ProcessorState.RUNNING;
        } finally {
            lock.unlock();
        }

        try {
            runInternal(process);
        } finally {
            runOver();
        }
    }

    @Override
    public void suspend() {
        lock.lock();
        try {
            if (state == ProcessorState.RUNNING) {
                if (resumeCondition == null) {
                    resumeCondition = lock.newCondition();
                }
                state = ProcessorState.SUSPENDED;
                quartz.interrupt();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void resume() {
        lock.lock();
        try {
            if (state == ProcessorState.SUSPENDED) {
                state = ProcessorState.RUNNING;
                resumeCondition.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void halt() {
        lock.lock();
        try {
            state = ProcessorState.HALTED;
            if (resumeCondition != null) {
                resumeCondition.signal();
            }
            quartz.interrupt();
        } finally {
            lock.unlock();
        }
    }

    private void runInternal(Process process) throws InterruptedException {
        execQueue.clear();
        schedule(process, 0L);
        while (execQueue.size() > 0) {
            execNext();
        }
    }

    private void runOver() {
        lock.lock();
        try {
            state = ProcessorState.HALTED;
            runningThreadId = 0L;
        } finally {
            lock.unlock();
        }
    }

    private void execNext() throws InterruptedException {
        ProcessorWrapper pw = execQueue.get(0);

        long now = quartz.currentTimeMillis();
        if (pw.nextExecTime > now) {
            quartz.sleep(pw.nextExecTime - now);
        }

        if (state == ProcessorState.HALTED) {
            execQueue.clear();
            return;
        }

        if (state == ProcessorState.SUSPENDED) {
            if (waitResume()) {
                return;
            }
        }

        execQueue.removeByIndex(0);
        long delay = pw.process.run();
        if (delay >= 0L) {
            pw.nextExecTime = quartz.currentTimeMillis() + delay;
            execQueue.add(pw);
        }
    }

    private void assertInvokerThreadId() {
        if (runningThreadId != Thread.currentThread().getId()) {
            throw new IllegalAccessError();
        }
    }

    private boolean waitResume() throws InterruptedException {
        lock.lock();
        try {
            if (state == ProcessorState.RUNNING) {
                return false;
            }
            while (state == ProcessorState.SUSPENDED) {
                resumeCondition.await();
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append("SingleThreadProcessor: {state=").append(state).append(", queueSize=")
                .append(execQueue.size()).append(", quartz=").append(quartz).append("}").toString();
    }
}
