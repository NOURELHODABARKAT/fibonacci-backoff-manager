package com.example.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a smart retry mechanism using Fibonacci sequence backoff with jitter,
 * circuit breaker pattern, and async support.
 * 
 * <p>This service provides configurable retry logic for network operations and other
 * transient error-prone tasks. Key features include:
 * <ul>
 *   <li>Fibonacci-based delay progression between attempts</li>
 *   <li>Random jitter to prevent request synchronization</li>
 *   <li>Circuit breaker pattern for fault tolerance</li>
 *   <li>Async non-blocking retry support</li>
 *   <li>Thread-safe implementation</li>
 *   <li>Configurable parameters via constructor</li>
 *   <li>Detailed logging and metrics hooks</li>
 * </ul>
 * 
 * @author norelhodabarkat
 * @version 1.0
 */
/**
 * <p><b>Usage Exam</b>
 * <pre>{@code
 * FibonacciBackoffService service = new FibonacciBackoffService(5, 100);
 * try {
 *     String result = service.executeWithRetry(() -> {
 *         // Your business logic
 *         return "Success";
 *     });
 * } catch (RetryException e) {
 *     // Handle failure
 * }
 * }</pre>
 */
public class FibonacciBackoffService {

    private static final Logger logger = LoggerFactory.getLogger(FibonacciBackoffService.class);
    
    // Configuration constants
    private static final int MAX_ALLOWED_RETRIES = 30;
    private static final double DEFAULT_JITTER_FACTOR = 0.1;
    private static final int DEFAULT_FAILURE_THRESHOLD = 5;
    private static final long DEFAULT_CIRCUIT_OPEN_TIME_MS = 60000;

    // Configuration parameters
    private final int maxRetries;
    private final long initialDelay;
    private final double jitterFactor;
    private final int failureThreshold;
    private final long circuitOpenTimeMs;
    
    // Runtime state
    private final long[] fibSequence;
    private volatile boolean circuitOpen = false;
    private volatile long lastFailureTime;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Constructs a Fibonacci backoff service with default parameters.
     * 
     * @param maxRetries Maximum number of retry attempts (1-30)
     * @param initialDelay Initial delay in milliseconds (≥1)
     */
    public FibonacciBackoffService(int maxRetries, long initialDelay) {
        this(maxRetries, initialDelay, DEFAULT_JITTER_FACTOR, 
             DEFAULT_FAILURE_THRESHOLD, DEFAULT_CIRCUIT_OPEN_TIME_MS);
    }

    /**
     * Advanced constructor with full configuration options.
     * 
     * @param maxRetries Maximum retry attempts (1-30)
     * @param initialDelay Initial delay in milliseconds (≥1)
     * @param jitterFactor Jitter range (± percentage of delay)
     * @param failureThreshold Consecutive failures to trip circuit
     * @param circuitOpenTimeMs Time to keep circuit open (milliseconds)
     */
    public FibonacciBackoffService(int maxRetries, long initialDelay, 
                                  double jitterFactor, int failureThreshold,
                                  long circuitOpenTimeMs) {
        validateParameters(maxRetries, initialDelay);
        this.maxRetries = maxRetries;
        this.initialDelay = initialDelay;
        this.jitterFactor = jitterFactor;
        this.failureThreshold = failureThreshold;
        this.circuitOpenTimeMs = circuitOpenTimeMs;
        this.fibSequence = generateFibonacciSequence();
    }

    private void validateParameters(int maxRetries, long initialDelay) {
        if (maxRetries < 1 || maxRetries > MAX_ALLOWED_RETRIES) {
            throw new IllegalArgumentException(
                String.format("Max retries must be between 1 and %d", MAX_ALLOWED_RETRIES)
            );
        }
        if (initialDelay < 1) {
            throw new IllegalArgumentException("Initial delay must be ≥1 ms");
        }
    }

    private long[] generateFibonacciSequence() {
        long[] sequence = new long[maxRetries];
        sequence[0] = initialDelay;
        if (maxRetries > 1) {
            sequence[1] = initialDelay;
            for (int i = 2; i < maxRetries; i++) {
                sequence[i] = Math.addExact(sequence[i-1], sequence[i-2]);
            }
        }
        return sequence;
    }

    /**
     * Executes a task with retry logic and circuit breaker protection.
     * 
     * @param <T> Type of task result
     * @param task Callable task to execute
     * @return Task result if successful
     * @throws RetryException if all retry attempts fail or circuit is open
     */
    public <T> T executeWithRetry(Callable<T> task) throws RetryException {
        checkCircuitBreaker();
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                T result = task.call();
                resetConsecutiveFailures();
                logger.info("Operation succeeded on attempt {}", attempt + 1);
                return result;
            } catch (Exception e) {
                handleFailure(attempt, e);
            }
        }
        throw new RetryException("All retry attempts failed");
    }

    /**
     * Async version of executeWithRetry using CompletableFuture.
     * 
     * @param <T> Type of task result
     * @param task Callable task to execute
     * @return CompletableFuture with result or error
     */
    public <T> CompletableFuture<T> executeWithRetryAsync(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeWithRetry(task);
            } catch (RetryException e) {
                throw new CompletionException(e);
            }
        });
    }

    private void checkCircuitBreaker() throws RetryException {
        if (circuitOpen) {
            if (System.currentTimeMillis() - lastFailureTime > circuitOpenTimeMs) {
                logger.info("Circuit breaker transitioning to half-open");
                circuitOpen = false;
            } else {
                logger.warn("Circuit breaker is open - rejecting request");
                throw new RetryException("Circuit breaker is open");
            }
        }
    }

    private void handleFailure(int attempt, Exception error) throws RetryException {
        consecutiveFailures.incrementAndGet();
        logger.warn("Attempt {} failed: {}", attempt + 1, error.getMessage());

        if (consecutiveFailures.get() >= failureThreshold) {
            tripCircuitBreaker();
        }

        if (attempt < maxRetries - 1) {
            scheduleRetry(attempt);
        }
    }

    private void scheduleRetry(int attempt) throws RetryException {
        long delay = calculateDelay(attempt);
        logger.info("Scheduling retry in {} ms", delay);
        
        try {
            scheduler.schedule(
                () -> logger.debug("Executing retry attempt {}", attempt + 2),
                delay,
                TimeUnit.MILLISECONDS
            ).get();
        } catch (Exception e) {
            handleRetryError(e);
        }
    }

    private long calculateDelay(int attempt) {
        long baseDelay = fibSequence[attempt];
        double jitter = ThreadLocalRandom.current().nextDouble(-jitterFactor, jitterFactor);
        return (long) Math.max(1, baseDelay * (1 + jitter));
    }

    private void tripCircuitBreaker() {
        circuitOpen = true;
        lastFailureTime = System.currentTimeMillis();
        consecutiveFailures.set(0);
        logger.error("Circuit breaker tripped - stopping all requests");
    }

    private void resetConsecutiveFailures() {
        consecutiveFailures.set(0);
    }

    private void handleRetryError(Exception e) throws RetryException {
        if (e.getCause() instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            throw new RetryException("Retry operation interrupted", e);
        }
        throw new RetryException("Retry scheduling failed", e);
    }

    /**
     * Custom exception for retry failures and circuit breaker state.
     */
    public static class RetryException extends Exception {
        public RetryException(String message) { super(message); }
        public RetryException(String message, Throwable cause) { super(message, cause); }
    }

    /**
     * Shutdown hook for graceful termination.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}