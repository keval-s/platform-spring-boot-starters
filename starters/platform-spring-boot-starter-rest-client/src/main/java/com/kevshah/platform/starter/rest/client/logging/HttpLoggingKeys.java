package com.kevshah.platform.starter.rest.client.logging;

/**
 * Structured log key constants for the REST client logging interceptor.
 *
 * <p>Every SLF4J {@code addKeyValue} call in {@link RestClientLoggingInterceptor} must reference a constant from this
 * class so that key names are defined in exactly one place and remain consistent across request and response entries.
 *
 * <p>All keys follow the pattern {@code platform.rest-client.<concern>}.
 */
public final class HttpLoggingKeys {

    /** Logical name of the REST client instance (e.g. {@code "payments-service"}). */
    public static final String CLIENT_NAME = "platform.rest-client.name";

    /** Human-readable label for the endpoint being called (e.g. {@code "createOrder"}). */
    public static final String ENDPOINT = "platform.rest-client.endpoint";

    /** HTTP method of the outbound request (e.g. {@code "GET"}, {@code "POST"}). */
    public static final String METHOD = "platform.rest-client.http.request.method";

    /** Full URL of the outbound request. */
    public static final String URL = "platform.rest-client.http.request.url";

    /** HTTP response status code (integer). */
    public static final String STATUS = "platform.rest-client.http.response.status";

    /** Round-trip duration in milliseconds from sending the request to receiving the response. */
    public static final String DURATION_MS = "platform.rest-client.http.duration-ms";

    /** Serialized request headers (present only when header logging is enabled). */
    public static final String REQUEST_HEADERS = "platform.rest-client.http.request.headers";

    /** Raw request body string (present only when request-body logging is enabled and body is non-empty). */
    public static final String REQUEST_BODY = "platform.rest-client.http.request.body";

    /** Serialized response headers (present only when header logging is enabled). */
    public static final String RESPONSE_HEADERS = "platform.rest-client.http.response.headers";

    /** Raw response body string (present only when response-body logging is enabled). */
    public static final String RESPONSE_BODY = "platform.rest-client.http.response.body";

    private HttpLoggingKeys() {}
}
