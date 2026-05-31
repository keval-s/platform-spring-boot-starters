package com.kevshah.platform.starter.rest.server;

import com.kevshah.platform.starter.rest.server.config.RestServerProperties;
import com.kevshah.platform.starter.rest.server.filter.StandardRequestResponseLoggingFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

/// Auto-configuration for `platform-spring-boot-starter-rest-server`.
///
/// Activates only in a web application context and registers platform-level
/// REST server infrastructure beans. Configuration is bound from the
/// `platform.rest.server` prefix via [RestServerProperties].
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(RestServerProperties.class)
public class RestServerAutoConfiguration {


    /// Registers the [StandardRequestResponseLoggingFilter] bean when request/response
    /// logging is explicitly enabled via `platform.rest.server.logging.enabled=true`.
    ///
    /// @param properties  the bound REST server configuration properties
    /// @param jsonMapper  the Jackson `JsonMapper` used to serialise log output
    /// @return a configured `StandardRequestResponseLoggingFilter` instance
    @Bean
    @ConditionalOnProperty(prefix = "platform.rest.server.logging", name = "enabled", havingValue = "true")
    public StandardRequestResponseLoggingFilter standardRequestResponseLoggingFilter(RestServerProperties properties, JsonMapper jsonMapper) {
        return new StandardRequestResponseLoggingFilter(properties, jsonMapper);
    }


}
