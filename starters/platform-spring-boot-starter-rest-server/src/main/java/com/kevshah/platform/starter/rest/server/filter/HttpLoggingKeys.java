package com.kevshah.platform.starter.rest.server.filter;

/// Structured log key constants for the REST server logging filter.
///
/// Every SLF4J `addKeyValue` call in [StandardRequestResponseLoggingFilter] must reference
/// a constant from this class so that key names are defined in exactly one place and remain
/// consistent across request and response entries.
///
/// All keys follow the pattern `platform.rest-server.http.<concern>`.
public final class HttpLoggingKeys {

    /// HTTP method of the incoming request (e.g. `"GET"`, `"POST"`).
    public static final String METHOD = "platform.rest-server.http.request.method";

    /// Request URI path (e.g. `"/api/orders/42"`).
    public static final String URL = "platform.rest-server.http.request.url";

    /// HTTP response status code (integer).
    public static final String STATUS = "platform.rest-server.http.response.status";

    /// Parsed or raw request body (present only when request body logging is enabled).
    public static final String REQUEST_BODY = "platform.rest-server.http.request.body";

    /// Parsed or raw response body (present only when response body logging is enabled).
    public static final String RESPONSE_BODY = "platform.rest-server.http.response.body";

    /// Map of request header names to values (present only when request header logging is enabled).
    public static final String REQUEST_HEADERS = "platform.rest-server.http.request.headers";

    /// Map of response header names to values (present only when response header logging is enabled).
    public static final String RESPONSE_HEADERS = "platform.rest-server.http.response.headers";

    private HttpLoggingKeys() {}
}
