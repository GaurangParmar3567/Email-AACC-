package com.example.mail.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit when 50% of requests fail
                .slidingWindowSize(10) // Evaluate last 10 requests
                .minimumNumberOfCalls(5) // Minimum calls before evaluating failure rate
                .waitDurationInOpenState(Duration.ofSeconds(60)) // Wait 60 seconds before half-open
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 test calls in half-open state
                .recordExceptions(Exception.class) // Record all exceptions as failures
                .ignoreExceptions(IllegalArgumentException.class) // Don't count these as failures
                .build();
    }

    @Bean
    public RetryConfig retryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3) // Retry up to 3 times
                .waitDuration(Duration.ofSeconds(2)) // Wait 2 seconds between retries
                .retryExceptions(Exception.class) // Retry on all exceptions
                .ignoreExceptions(IllegalArgumentException.class) // Don't retry on these
                .retryOnResult(response -> false) // Don't retry based on result
                .build();
    }

    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(30)) // Timeout after 30 seconds
                .build();
    }
}