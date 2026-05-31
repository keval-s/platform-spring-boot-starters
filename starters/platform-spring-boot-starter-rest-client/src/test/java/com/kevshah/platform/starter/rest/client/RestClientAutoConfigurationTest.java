package com.kevshah.platform.starter.rest.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RestClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    com.kevshah.platform.starter.rest.client.RestClientAutoConfiguration.class));

    @Nested
    class WhenNoClientsConfigured {

        @Test
        void platformRestClientRegistry_noClientsProperty_registryBeanIsPresent() {
            // Given/When
            contextRunner.run(context -> {
                // Then
                assertThat(context).hasSingleBean(PlatformRestClientRegistry.class);
            });
        }

        @Test
        void platformRestClientRegistry_noClientsProperty_registryHasNoClientNames() {
            // Given/When
            contextRunner.run(context -> {
                // Then
                var registry = context.getBean(PlatformRestClientRegistry.class);
                assertThat(registry.getClientNames()).isEmpty();
            });
        }
    }

    @Nested
    class WhenClientsConfigured {

        @Test
        void platformRestClientRegistry_singleClientConfigured_registryContainsClientName() {
            // Given/When
            contextRunner
                    .withPropertyValues(
                            "platform.rest.client.clients.payment-service.base-url=http://localhost:8080"
                    )
                    .run(context -> {
                        // Then
                        assertThat(context).hasSingleBean(PlatformRestClientRegistry.class);
                        var registry = context.getBean(PlatformRestClientRegistry.class);
                        assertThat(registry.getClientNames()).containsExactly("payment-service");
                    });
        }

        @Test
        void platformRestClientRegistry_multipleClientsConfigured_registryContainsAllClientNames() {
            // Given/When
            contextRunner
                    .withPropertyValues(
                            "platform.rest.client.clients.payment-service.base-url=http://payment:8080",
                            "platform.rest.client.clients.order-service.base-url=http://order:8081"
                    )
                    .run(context -> {
                        // Then
                        var registry = context.getBean(PlatformRestClientRegistry.class);
                        assertThat(registry.getClientNames())
                                .containsExactlyInAnyOrder("payment-service", "order-service");
                    });
        }

        @Test
        void platformRestClientRegistry_clientWithRetryConfig_registryContainsClientName() {
            // Given/When
            contextRunner
                    .withPropertyValues(
                            "platform.rest.client.clients.payment-service.base-url=http://localhost:8080",
                            "platform.rest.client.clients.payment-service.retry.max-attempts=3",
                            "platform.rest.client.clients.payment-service.retry.wait-duration=500ms",
                            "platform.rest.client.clients.payment-service.retry.retry-on-response-statuses=503,504"
                    )
                    .run(context -> {
                        // Then
                        assertThat(context).hasSingleBean(PlatformRestClientRegistry.class);
                        var registry = context.getBean(PlatformRestClientRegistry.class);
                        assertThat(registry.getClientNames()).containsExactly("payment-service");
                    });
        }

        @Test
        void platformRestClientRegistry_clientWithEndpoints_endpointIsRetrievable() {
            // Given/When
            contextRunner
                    .withPropertyValues(
                            "platform.rest.client.clients.payment-service.base-url=http://localhost:8080",
                            "platform.rest.client.clients.payment-service.endpoints.create-payment.method=POST",
                            "platform.rest.client.clients.payment-service.endpoints.create-payment.path=/api/v1/payments"
                    )
                    .run(context -> {
                        // Then
                        var registry = context.getBean(PlatformRestClientRegistry.class);
                        var endpoint = registry.getEndpoint("payment-service", "create-payment");
                        assertThat(endpoint.method()).isEqualTo("POST");
                        assertThat(endpoint.path()).isEqualTo("/api/v1/payments");
                    });
        }
    }

    @Nested
    class BeanNaming {

        @Test
        void platformRestClientRegistry_beanNameIsUnique_beanRegisteredUnderExpectedName() {
            // Given/When
            contextRunner.run(context -> {
                // Then
                assertThat(context.containsBean("platformRestClientRegistry")).isTrue();
            });
        }
    }
}


