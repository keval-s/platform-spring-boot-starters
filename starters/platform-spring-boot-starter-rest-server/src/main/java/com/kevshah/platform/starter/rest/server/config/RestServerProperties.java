package com.kevshah.platform.starter.rest.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/// Top-level configuration properties for `platform-spring-boot-starter-rest-server`.
/// All properties are nested under the `platform.rest.server` prefix.
///
/// @param logging Configuration properties for request/response logging. See [RestServerLoggingProperties].
@ConfigurationProperties(prefix = "platform.rest.server")
public record RestServerProperties(@NestedConfigurationProperty RestServerLoggingProperties logging) {

}
