package com.kevshah.platform.starter.rest.client.config;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Per-client configuration for a named REST client.
 *
 * <p>Each entry under {@code platform.rest.client.clients} maps to one instance of this record.
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * platform:
 * rest:
 *   client:
 *     clients:
 *       payment-service:
 *         base-url: https://payment.example.com
 *         default-content-type: application/json
 *         default-accept: application/json
 *         connect-timeout: 5s
 *         read-timeout: 30s
 *         endpoints:
 *           create-payment:
 *             method: POST
 *             path: /api/v1/payments
 *         retry:
 *           max-attempts: 3
 *           wait-duration: 500ms
 *         ssl:
 *           bundle: payment-service-ssl
 *         logging:
 *           enabled: true
 *           request-body: true
 *           response-body: true
 *           level: DEBUG
 * }</pre>
 *
 * @param baseUrl Base URL of the external service (e.g. {@code https://payment.example.com}).
 * @param defaultContentType Default {@code Content-Type} header value applied to every request made by this client.
 * @param defaultAccept Default {@code Accept} header value applied to every request made by this client.
 * @param defaultQueryParams Default query parameters appended to every request made by this client. Endpoint-level
 *     {@code default-query-params} are merged on top of these values.
 * @param connectTimeout Connection timeout for establishing a TCP connection to the remote host.
 * @param readTimeout Read timeout for waiting for a response after the connection is established.
 * @param endpoints Named endpoint definitions for this client. Each key becomes the logical name used to look up the
 *     endpoint via {@code PlatformRestClientRegistry.getEndpoint(clientName, endpointName)}.
 * @param retry Retry configuration. When absent, requests are attempted exactly once.
 * @param ssl SSL bundle configuration. When absent, the JVM's default SSL context is used.
 * @param logging Request/response logging configuration for all endpoints of this client. Endpoint-level
 *     {@code logging} settings are merged on top of these values.
 */
public record ClientProperties(
        String baseUrl,
        String defaultContentType,
        String defaultAccept,
        Map<String, String> defaultQueryParams,
        Duration connectTimeout,
        Duration readTimeout,
        Map<String, EndpointProperties> endpoints,
        @NestedConfigurationProperty RetryProperties retry,
        @NestedConfigurationProperty SslProperties ssl,
        @NestedConfigurationProperty LoggingProperties logging) {}
