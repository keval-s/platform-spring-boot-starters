package com.kevshah.platform.starter.rest.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Top-level configuration properties for {@code platform-spring-boot-starter-rest-server}.
 *
 * <p>All properties are nested under the {@code platform.rest.server} prefix.
 *
 * @param logging Configuration properties for request/response logging. See {@link LoggingProperties}.
 */
@ConfigurationProperties(prefix = "platform.rest.server")
public record RestServerProperties(
        @NestedConfigurationProperty LoggingProperties logging) {}
