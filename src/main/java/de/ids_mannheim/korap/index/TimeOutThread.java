package de.ids_mannheim.korap.index;

import org.apache.lucene.util.Counter;
import java.lang.*;
import java.lang.InterruptedException.*;
import org.apache.lucene.util.ThreadInterruptedException;

/**
 * Create a timer thread for search time outs.
 */

// See TimeLimitingCollector
public class TimeOutThread extends Thread {
    private static final long resolution = 250;
    private volatile boolean stop = false;
    private Counter counter;


    public TimeOutThread () {
        super("TimeOutThread");
        counter = Counter.newCounter(true);
    };


    @Override
    public void run () {
        while (!stop) {
            counter.addAndGet(resolution);
            try {
                Thread.sleep(resolution);
            }
            catch (InterruptedException ie) {
                throw new ThreadInterruptedException(ie);
            };
        };
    };


    // Get miliseconds
    public long getTime () {
        return counter.get();
    };


    // Stops the timer thread 
    public void stopTimer () {
        stop = true;
    };
};
