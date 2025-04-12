package com.example.retry;

import com.example.policy.RetryPolicy; 
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

public interface RetryListener {
    void beforeRetry(int attempt, long delay);
    void onRetryFailure(Exception e);
}

class FibonacciRetryManagerTest {

    @Test
    void testRetries() {
        AtomicInteger tries = new AtomicInteger(0);

        FibonacciRetryManager manager = new FibonacciRetryManager(
            new RetryPolicy(5),
            new RetryListener() {
                public void beforeRetry(int attempt, long delay) {
                    // This method is intentionally left empty because no action is needed before retry.
                }
                public void onRetryFailure(Exception e) {
                    fail("Should not fail yet");
                }
            }
        );

        manager.executeWithRetry(() -> {
            if (tries.incrementAndGet() < 3) throw new RuntimeException("fail");
        });

        assertEquals(3, tries.get());
    }
}

