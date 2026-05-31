package com.kevshah.platform.starter.rest.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "platform.rest.server")
public record RestServerProperties(@NestedConfigurationProperty RestServerLoggingProperties logging) {

}
