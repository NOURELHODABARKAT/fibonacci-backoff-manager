## Configuration Options
| Parameter | Default | Description |
|-----------|---------|-------------|
| maxRetries | 5 | Maximum retry attempts (1-30) |
| initialDelay | 100ms | Initial delay duration |
| jitterFactor | 0.1 | ±10% jitter range |
| failureThreshold | 5 | Failures to trip circuit |
| circuitOpenTimeMs | 60000 | Circuit breaker timeout |