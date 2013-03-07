package org.m43c.simulator;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class QuartzTest {
    
    private Quartz quartz = new Quartz();
    
    @Test
    public void testInfinity() throws InterruptedException {
        quartz.setK(-10000L);
        long now = quartz.currentTimeMillis();
        Thread.sleep(1);
        assertEquals(now, quartz.currentTimeMillis());
    }
    
    @Test(expectedExceptions={IllegalArgumentException.class})
    public void setKTest() {
        quartz.setK(0L);
    }
    
    @Test
    public void fastSleepTest() throws InterruptedException {
        quartz.setK(100);
        long start = System.currentTimeMillis();
        quartz.sleep(5000L);
        long diff = System.currentTimeMillis() - start;
        assertTrue(diff >= 50L);
        assertTrue(diff < 500L);
    }
    
    @Test
    public void slowSleepTest() throws InterruptedException {
        quartz.setK(-10);
        long start = System.currentTimeMillis();
        quartz.sleep(5L);
        long diff = System.currentTimeMillis() - start;
        assertTrue(diff >= 50L);
    }
    
    @Test
    public void fullTrottleTest() throws InterruptedException {
        quartz.setK(Quartz.FULL_THROTTLE);
        long time = quartz.currentTimeMillis();
        assertEquals(time + 1L, quartz.currentTimeMillis());
        assertEquals(time + 2L, quartz.currentTimeMillis());
        quartz.sleep(997L);
        assertEquals(time + 1000L, quartz.currentTimeMillis());
    }
    
    @Test(timeOut = 1000L)
    public void resetTimeTest() throws InterruptedException {
        long sleepTime = 100000000L;
        quartz.setK(Quartz.FULL_THROTTLE);
        quartz.sleep(sleepTime);
        assertTrue(quartz.currentTimeMillis() - quartz.getResetTime() > sleepTime);
        quartz.reset();
        assertTrue(quartz.currentTimeMillis() - quartz.getResetTime() == 1);
    }
    
    @Test(timeOut = 5000L)
    public void interruptTest() throws InterruptedException {
        quartz.setK(-10);
        new Thread(new Runnable() {
            @Override
            public void run() {
                quartz.interrupt();
            }
        }).start();
        quartz.sleep(100000L);
    }
}
