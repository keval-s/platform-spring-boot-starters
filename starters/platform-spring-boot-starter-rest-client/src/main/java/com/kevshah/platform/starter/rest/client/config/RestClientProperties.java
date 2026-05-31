package com.kevshah.platform.starter.rest.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/// Top-level configuration properties for `platform-spring-boot-starter-rest-client`.
///
/// All properties are nested under the `platform.rest.client` prefix.
/// Each key in `clients` names a logical REST client and maps to a `ClientProperties`
/// record describing its base URL, endpoints, retry settings, and SSL bundle.
///
/// @param clients Named REST client definitions. Each key becomes the logical client name used throughout `PlatformRestClientRegistry` method calls.
@ConfigurationProperties(prefix = "platform.rest.client")
public record RestClientProperties(
        Map<String, ClientProperties> clients
) {
}

