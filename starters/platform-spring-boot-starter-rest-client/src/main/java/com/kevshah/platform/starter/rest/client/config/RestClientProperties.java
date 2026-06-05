package com.kevshah.platform.starter.rest.client.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Top-level configuration properties for {@code platform-spring-boot-starter-rest-client}.
 *
 * <p>All properties are nested under the {@code platform.rest.client} prefix. Each key in {@code clients} names a
 * logical REST client and maps to a {@code ClientProperties} record describing its base URL, endpoints, retry settings,
 * and SSL bundle.
 *
 * @param clients Named REST client definitions. Each key becomes the logical client name used throughout
 *     {@code PlatformRestClientRegistry} method calls.
 */
@ConfigurationProperties(prefix = "platform.rest.client")
public record RestClientProperties(Map<String, ClientProperties> clients) {}
