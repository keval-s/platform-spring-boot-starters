package com.kevshah.platform.starter.rest.client.logging;

import com.kevshah.platform.starter.rest.client.config.LoggingProperties;

/// Bundles the resolved logging configuration with the identifiers of the client and
/// endpoint that produced it.
///
/// Set as a request attribute by `PlatformRestClient` before each call and read by
/// [RestClientLoggingInterceptor] to determine what to log and at what level.
///
/// @param clientName   logical name of the REST client
/// @param endpointName logical name of the endpoint being invoked
/// @param config       merged logging configuration effective for this call
public record LoggingContext(String clientName, String endpointName, LoggingProperties config) {}
