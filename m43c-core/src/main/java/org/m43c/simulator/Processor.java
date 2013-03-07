package org.m43c.simulator;

public interface Processor {

    /**
     * For running only
     * 
     * @param process
     * @param delayMs
     */
    void schedule(Process process, long delayMs);
    
    /**
     * executes until there are processes in the queue. Synchronous blocking call 
     */
    void run(Process process) throws InterruptedException;
    
    /**
     * suspends execution (only if running)
     */
    void suspend();
    
    /**
     * Resumes suspended proces
     */
    void resume();
    
    /**
     * Stops running, if runs. 
     */
    void halt();
    
}
