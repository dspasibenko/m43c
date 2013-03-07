package org.m43c.simulator;

import org.jrivets.env.TimeSource;

/**
 * Virtual time generator for single thread.
 * 
 * This implementation intends for using from a single thread only, including
 * {@code TimeSource} interface methods. Only one method can be invoked safely
 * from any other thread which is {@code interrupt()}
 * 
 * @author Dmitry Spasibenko
 * 
 */
final class Quartz implements TimeSource {

    static long FULL_THROTTLE = Long.MAX_VALUE;

    private volatile long k = 1L;

    private long currentTimeMs;

    private long resetTimeMs;

    private long realCheckPoint;

    private final Object syncObject = new Object();

    private boolean interrupted;

    private boolean sleeping;

    Quartz() {
        reset();
    }

    @Override
    public long currentTimeMillis() {
        calcCurrentTimeMs();
        return currentTimeMs;
    }

    void setK(long k) {
        if (k == 0) {
            throw new IllegalArgumentException("Time acceleration coefficient cannot be 0!");
        }
        if (this.k == FULL_THROTTLE) {
            this.realCheckPoint = System.nanoTime();
        }
        this.k = k;
        interrupt(false);
    }

    void reset() {
        currentTimeMs = System.currentTimeMillis();
        resetTimeMs = currentTimeMs;
        realCheckPoint = System.nanoTime();
        interrupted = false;
    }

    long getResetTime() {
        return resetTimeMs;
    }

    /**
     * Puts the invocation thread for sleep for millis in the quartz time.
     * 
     * <p>
     * So as this is a single-thread oriented implementation only one thread can
     * be on sleep at a time. The thread should also perform other activities
     * with the class, otherwise the behavior is not predictable.
     * 
     * @param millis
     *            - the "quartz" milliseconds.
     * @return true if the sleep is not interrupted by the {@code interrupt()}
     * @throws InterruptedException
     */
    boolean sleep(long millis) throws InterruptedException {
        boolean result = true;
        if (k == FULL_THROTTLE) {
            currentTimeMs += millis;
        } else {
            if (k > 0) {
                millis /= k;
            } else {
                millis *= -k;
            }
            if (millis > 0) {
                result = sleepInterruptable(millis);
            }
        }
        return result;
    }

    /**
     * Causes to return back from
     * {@code sleep()) if an other thread is blocked there. 
     */
    void interrupt() {
        interrupt(true);
    }

    private void interrupt(boolean evenIfNotSleeping) {
        synchronized (syncObject) {
            syncObject.notifyAll();
            if (evenIfNotSleeping || sleeping) {
                interrupted = true;
            }
        }
    }

    private boolean sleepInterruptable(long millis) throws InterruptedException {
        synchronized (syncObject) {
            sleeping = true;
            try {
                long sleepTill = System.currentTimeMillis() + millis;
                while (!interrupted && millis > 0) {
                    syncObject.wait(millis);
                    millis = sleepTill - System.currentTimeMillis();
                }
                return interrupted;
            } finally {
                sleeping = false;
                interrupted = false;
            }
        }
    }

    private void calcCurrentTimeMs() {
        if (k == FULL_THROTTLE) {
            currentTimeMs++;
        } else {
            long now = System.nanoTime();
            long realDiff = now - realCheckPoint;
            if (k > 0) {
                currentTimeMs += (realDiff * k) / 1000000L;
            } else {
                currentTimeMs -= (realDiff / k) / 1000000L;
            }
            realCheckPoint = now;
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{k=").append((k == FULL_THROTTLE ? "FULL_THROTTLE" : k))
                .append(", currentTimeMs=").append(currentTimeMs).append(", interrupted=").append(interrupted)
                .append("}").toString();
    }

}
