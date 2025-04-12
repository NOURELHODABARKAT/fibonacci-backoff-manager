package com.example.retry;

import com.example.policy.RetryPolicy;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages retry logic using a Fibonacci backoff strategy.
 * Supports executing Runnable and Callable tasks with automatic retries and delay handling.
 */
public class FibonacciRetryManager {
    private final RetryPolicy retryPolicy;
    private final RetryListener retryListener;
    private static final Logger logger = LoggerFactory.getLogger(FibonacciRetryManager.class);

    /**
     * Constructs a FibonacciRetryManager with the specified policy and listener.
     *
     * @param retryPolicy    the policy defining retry limits and delay logic
     * @param retryListener  a listener for retry lifecycle events
     */
    public FibonacciRetryManager(RetryPolicy retryPolicy, RetryListener retryListener) {
        this.retryPolicy = retryPolicy;
        this.retryListener = retryListener;
    }

    /**
     * Executes a Runnable task with retry logic.
     *
     * @param task the Runnable task to execute
     */
    public void executeWithRetry(Runnable task) {
        int attempt = 0;
        logger.info("🔁 Starting retry for Runnable...");

        while (attempt < retryPolicy.getMaxRetries()) {
            try {
                task.run();
                logger.info("✅ Task succeeded on attempt {}", attempt + 1);
                return;
            } catch (Exception e) {
                attempt++;
                logger.warn("⚠️ Attempt {} failed", attempt, e);

                if (attempt >= retryPolicy.getMaxRetries()) {
                    logger.error("❌ Retry failed after {} attempts", attempt, e);
                    retryListener.onRetryFailure(e);
                    throw e;
                }

                long delay = retryPolicy.getDelay(attempt);
                logger.info("⏳ Waiting {} ms before next retry...", delay);
                retryListener.beforeRetry(attempt, delay);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    logger.warn("⚠️ Thread interrupted during delay.");
                }
            }
        }
    }

    /**
     * Executes a Callable task with retry logic and returns its result.
     *
     * @param task the Callable task to execute
     * @param <T>  the result type
     * @return the result from the task
     * @throws Exception if all retries fail
     */
    public <T> T executeWithRetry(Callable<T> task) throws Exception {
        int attempt = 0;
        logger.info("🔁 Starting retry for Callable...");

        while (attempt < retryPolicy.getMaxRetries()) {
            try {
                T result = task.call();
                logger.info("✅ Task succeeded on attempt {}", attempt + 1);
                return result;
            } catch (Exception e) {
                attempt++;
                logger.warn("⚠️ Attempt {} failed", attempt, e);

                if (attempt >= retryPolicy.getMaxRetries()) {
                    logger.error("❌ Retry failed after {} attempts", attempt, e);
                    retryListener.onRetryFailure(e);
                    throw e;
                }

                long delay = retryPolicy.getDelay(attempt);
                logger.info("⏳ Waiting {} ms before next retry...", delay);
                retryListener.beforeRetry(attempt, delay);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    logger.warn("⚠️ Thread interrupted during delay.");
                }
            }
        }
        return null;
    }
}
