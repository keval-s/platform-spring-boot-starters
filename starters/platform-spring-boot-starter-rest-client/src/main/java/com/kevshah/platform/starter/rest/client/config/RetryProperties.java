package com.kevshah.platform.starter.rest.client.config;

import java.time.Duration;
import java.util.List;

/// Retry configuration for a named REST client.
///
/// When `maxAttempts` is greater than 1, failed requests matching `retryOnResponseStatuses`
/// (or any exception) are retried with either fixed or exponential back-off.
///
/// Example YAML:
/// ```yaml
/// retry:
///   max-attempts: 3
///   wait-duration: 500ms
///   multiplier: 2.0
///   max-wait-duration: 5s
///   retry-on-response-statuses: [429, 503, 504]
/// ```
///
/// @param maxAttempts             Total number of attempts (first attempt + retries). Defaults to `3` when not set.
/// @param waitDuration            Base wait duration between retry attempts. Defaults to `500ms` when not set.
/// @param multiplier              Multiplier applied to `waitDuration` for exponential back-off. A value of `1.0` (or omitting this field) uses fixed back-off.
/// @param maxWaitDuration         Maximum wait duration cap when exponential back-off is active. Ignored when `multiplier` is `1.0` or absent.
/// @param retryOnResponseStatuses HTTP response status codes that should trigger a retry. When a response arrives with one of these codes a `PlatformHttpStatusRetryException` is thrown so the retry template can re-execute the request.
public record RetryProperties(
        Integer maxAttempts,
        Duration waitDuration,
        Double multiplier,
        Duration maxWaitDuration,
        List<Integer> retryOnResponseStatuses
) {
}

