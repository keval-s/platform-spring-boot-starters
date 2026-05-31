package com.kevshah.platform.starter.rest.client.exception;

import java.net.URI;

/// Thrown by a `RestClient` status handler when the server responds with an HTTP status
/// code that has been configured for retry via
/// `platform.rest.client.clients.*.retry.retry-on-response-statuses`.
///
/// `PlatformRestClientRegistry.executeWithRetry` configures its `RetryTemplate` to
/// retry on this exception, so callers do not need to catch or handle it directly.
public final class PlatformHttpStatusRetryException extends RuntimeException {

    private final int statusCode;
    private final URI requestUri;

    /// Constructs a new instance for the given HTTP `statusCode` and `requestUri`.
    public PlatformHttpStatusRetryException(int statusCode, URI requestUri) {
        super("Retryable HTTP " + statusCode + " received for request: " + requestUri);
        this.statusCode = statusCode;
        this.requestUri = requestUri;
    }

    /// Returns the HTTP status code that triggered this exception.
    public int getStatusCode() {
        return statusCode;
    }

    /// Returns the URI of the request that produced the retryable status code.
    public URI getRequestUri() {
        return requestUri;
    }
}

