package com.kevshah.platform.starter.rest.server;

import com.kevshah.platform.starter.rest.server.config.RestServerProperties;
import com.kevshah.platform.starter.rest.server.filter.StandardRequestResponseLoggingFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(RestServerProperties.class)
public class RestServerAutoConfiguration {


    @Bean
    @ConditionalOnProperty(prefix = "platform.rest.server.logging", name = "enabled", havingValue = "true")
    public StandardRequestResponseLoggingFilter standardRequestResponseLoggingFilter(RestServerProperties properties, JsonMapper jsonMapper) {
        return new StandardRequestResponseLoggingFilter(properties, jsonMapper);
    }


}
