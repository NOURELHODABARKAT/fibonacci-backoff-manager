## Error Handling
### Handling Different Failure Scenarios
```java
try {
    service.executeWithRetry(...);
} catch (RetryException e) {
    if (e.getMessage().contains("Circuit breaker")) {
        // Handle circuit open state
    } else if (e.getCause() instanceof InterruptedException) {
        // Handle interruption
    } else {
        // Handle regular failure
    }
}