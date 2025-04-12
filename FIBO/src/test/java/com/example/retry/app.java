package com.example.retry;

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class app {
    private static final Logger logger = LoggerFactory.getLogger(app.class);
    
    public static void main(String[] args) {
        final int MAX_RETRIES = 5;
        final long INITIAL_DELAY = 100;
        
        try {
            String result = new FibonacciBackoffService(MAX_RETRIES, INITIAL_DELAY)
                .executeWithRetry(createMockAPICall());
            logger.info("Final result: {}", result);
        } catch (Exception e) {
            logger.error("Critical failure: {}", e.getMessage());
        }
    }

    private static Callable<String> createMockAPICall() {
        return () -> {
            if (Math.random() > 0.3) {
                throw new RuntimeException("API request timeout");
            }
            return "API response payload";
        };
    }
} 
    

