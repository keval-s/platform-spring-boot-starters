package com.kevshah.platform.starter.rest.client;

import com.kevshah.platform.starter.rest.client.config.RestClientProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/// Auto-configuration for `platform-spring-boot-starter-rest-client`.
///
/// Activates when `RestClient` is on the classpath (i.e. `spring-boot-starter-web`
/// or `spring-boot-starter-webflux` is present).
///
/// Registers a single `PlatformRestClientRegistry` bean named
/// `platformRestClientRegistry` that holds one pre-built [RestClient] and one
/// [RetryTemplate] per entry declared under `platform.rest.client.clients`.
@AutoConfiguration
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(RestClientProperties.class)
public class RestClientAutoConfiguration {

    /// Creates and registers the `PlatformRestClientRegistry` bean.
    ///
    /// `SslBundles` is injected via `ObjectProvider`-style nullable parameter so that
    /// the bean is still registered even when no SSL bundles are configured in the
    /// application.
    @Bean("platformRestClientRegistry")
    public PlatformRestClientRegistry platformRestClientRegistry(
            RestClientProperties properties,
            ObjectProvider<SslBundles> sslBundlesProvider) {
        return new PlatformRestClientRegistry(properties, sslBundlesProvider.getIfAvailable());
    }
}

