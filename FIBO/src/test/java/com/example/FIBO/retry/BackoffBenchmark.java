package com.example.FIBO.retry;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class BackoffBenchmark {

    private FibonacciBackoffService service;

    @Setup
    public void setup() {
        service = new FibonacciBackoffService(5, 50);
    }

    @Benchmark
    public void benchmarkRetries() throws Exception {
        service.executeWithRetry(() -> {
            if (Math.random() > 0.3) throw new Exception("Simulated Failure");
            return null;
        });
    }
}
