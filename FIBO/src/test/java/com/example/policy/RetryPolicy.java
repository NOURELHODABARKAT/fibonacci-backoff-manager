package com.example.policy;

public class RetryPolicy {
    private final int maxRetries;

    public RetryPolicy(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getDelay(int attempt) {
        if (attempt <= 1) return 1000L; 
        long a = 1000L, b = 1000L; 
        for (int i = 2; i < attempt; i++) {
            long temp = b;
            b = a + b;
            a = temp;
        }
        return b;
    }
}
