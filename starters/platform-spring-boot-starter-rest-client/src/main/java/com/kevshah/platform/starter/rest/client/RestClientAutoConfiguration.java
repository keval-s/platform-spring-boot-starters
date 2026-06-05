package com.kevshah.platform.starter.rest.client;

import com.kevshah.platform.starter.rest.client.config.RestClientProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Auto-configuration for {@code platform-spring-boot-starter-rest-client}.
 *
 * <p>Activates when {@code RestClient} is on the classpath (i.e. {@code spring-boot-starter-restclient},
 * {@code spring-boot-starter-web}, or {@code spring-boot-starter-webflux} is present).
 *
 * <p>Registers a single {@code PlatformRestClientRegistry} bean named {@code platformRestClientRegistry} that holds one
 * pre-built {@link RestClient} and one {@code RetryTemplate} per entry declared under
 * {@code platform.rest.client.clients}.
 */
@AutoConfiguration
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(RestClientProperties.class)
public class RestClientAutoConfiguration {

    /**
     * Creates and registers the {@code PlatformRestClientRegistry} bean.
     *
     * <p>{@code SslBundles} is injected via {@code ObjectProvider}-style nullable parameter so that the bean is still
     * registered even when no SSL bundles are configured in the application.
     *
     * @param properties the bound {@code RestClientProperties} configuration
     * @param sslBundlesProvider the provider for {@code SslBundles}
     * @return a new {@code PlatformRestClientRegistry} instance
     */
    @Bean("platformRestClientRegistry")
    public PlatformRestClientRegistry platformRestClientRegistry(
            RestClientProperties properties, ObjectProvider<SslBundles> sslBundlesProvider) {
        return new PlatformRestClientRegistry(properties, sslBundlesProvider.getIfAvailable());
    }
}
