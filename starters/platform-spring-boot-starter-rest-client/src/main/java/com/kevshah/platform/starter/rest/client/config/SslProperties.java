package com.kevshah.platform.starter.rest.client.config;

/// SSL configuration for a named REST client.
///
/// The `bundle` field references a named SSL bundle defined under `spring.ssl.bundle.*`
/// in the application configuration.
///
/// @param bundle Name of the SSL bundle (as configured under `spring.ssl.bundle.*`) to apply
///               to this client's connections.
public record SslProperties(
        String bundle
) {
}

