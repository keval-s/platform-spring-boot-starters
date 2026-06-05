package com.kevshah.platform.starter.rest.server;

import com.kevshah.platform.starter.rest.server.config.RestServerProperties;
import com.kevshah.platform.starter.rest.server.filter.StandardRequestResponseLoggingFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

/**
 * Autoconfiguration for {@code platform-spring-boot-starter-rest-server}.
 *
 * <p>Activates only in a web application context and registers platform-level REST server infrastructure beans.
 * Configuration is bound from the {@code platform.rest.server} prefix via {@link RestServerProperties}.
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(RestServerProperties.class)
public class RestServerAutoConfiguration {

    /**
     * Registers the {@link StandardRequestResponseLoggingFilter} bean when request/response logging is explicitly
     * enabled via {@code platform.rest.server.logging.enabled=true}.
     *
     * @param properties the bound REST server configuration properties
     * @param jsonMapper the Jackson {@code JsonMapper} used to serialize log output
     * @return a configured {@code StandardRequestResponseLoggingFilter} instance
     */
    @Bean
    @ConditionalOnProperty(prefix = "platform.rest.server.logging", name = "enabled", havingValue = "true")
    public StandardRequestResponseLoggingFilter standardRequestResponseLoggingFilter(
            RestServerProperties properties, JsonMapper jsonMapper) {
        return new StandardRequestResponseLoggingFilter(properties, jsonMapper);
    }
}
