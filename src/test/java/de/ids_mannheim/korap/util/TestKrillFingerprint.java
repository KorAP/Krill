package de.ids_mannheim.korap.util;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

public class TestKrillFingerprint {

    @Test
    public void testMessageDigestThreadSafety() throws Exception {

        int threads = 16;
        int iterationsPerThread = 5_000;

        ExecutorService pool = Executors.newFixedThreadPool(threads);

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);

        List<Throwable> errors = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < iterationsPerThread; i++) {
                        Fingerprinter.create("key-" + i);
                    }
                }
                catch (Throwable e) {
                    synchronized (errors) {
                        errors.add(e);
                    }
                }
                finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await();
        pool.shutdown();

        if (!errors.isEmpty()) {
            errors.forEach(Throwable::printStackTrace);
        }

        assertTrue(
            "Thread-safety violation detected: " + errors,
            errors.isEmpty()
        );
    }
}
