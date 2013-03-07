package org.m43c.simulator;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SingleThreadProcessorTest {

    private SingleThreadProcessor processor;
    
    @BeforeMethod
    public void init() {
        processor = new SingleThreadProcessor();
    }
    
    @Test(expectedExceptions={IllegalAccessError.class})
    public void scheduleChecksRunThreadId() {
        processor.schedule(null, 0L);
    }
    
    @Test(expectedExceptions={NullPointerException.class})
    public void runNPE() throws InterruptedException {
        processor.run(null);
    }
    
    @Test(timeOut=1000L)
    public void simpleRun() throws InterruptedException {
        processor.run(new Process() {
            @Override
            public long run() {
                return -1L;
            }
        });
    }
    
    @Test(expectedExceptions={NullPointerException.class})
    public void scheduleNPE() throws InterruptedException {
        processor.run(new Process() {
            @Override
            public long run() {
                processor.schedule(null, 0L);
                return -1L;
            }
        });
    }
}
