package com.kevshah.platform.starter.rest.client;

import com.kevshah.platform.starter.rest.client.config.ClientProperties;
import com.kevshah.platform.starter.rest.client.config.EndpointProperties;
import com.kevshah.platform.starter.rest.client.config.LoggingProperties;
import com.kevshah.platform.starter.rest.client.config.RestClientProperties;
import com.kevshah.platform.starter.rest.client.config.RetryProperties;
import com.kevshah.platform.starter.rest.client.exception.PlatformHttpStatusRetryException;
import com.kevshah.platform.starter.rest.client.logging.RestClientLoggingInterceptor;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Central registry that holds all pre-configured {@code RestClient} instances and their corresponding
 * {@code RetryTemplate}s.
 *
 * <p>One {@code RestClient} and one {@code RetryTemplate} are created per named client declared under
 * {@code platform.rest.client.clients.*}. Consumers inject this bean by type and obtain a {@code PlatformRestClient}
 * scoped to a specific named client:
 *
 * <p>
 *
 * <pre>{@code
 * @Autowired PlatformRestClientRegistry registry;
 *
 * PlatformRestClient paymentClient = registry.getPlatformRestClient("payment-service");
 *
 * var result = paymentClient.post("create-payment", request, PaymentResponse.class);
 * }</pre>
 *
 * <p>The bean is registered under the name {@code platformRestClientRegistry} to avoid collisions with any other beans
 * in the application context.
 */
public final class PlatformRestClientRegistry {

    private final Map<String, RestClient> clients;
    private final Map<String, RetryTemplate> retryTemplates;
    private final RestClientProperties properties;

    /**
     * Constructs the registry and eagerly builds all configured clients.
     *
     * @param properties the bound {@code @ConfigurationProperties} record.
     * @param sslBundles optional; when {@code null} the JVM's default SSL context is used for all clients.
     */
    public PlatformRestClientRegistry(RestClientProperties properties, SslBundles sslBundles) {
        this.properties = properties;

        var clientMap = new LinkedHashMap<String, RestClient>();
        var retryMap = new LinkedHashMap<String, RetryTemplate>();

        if (properties.clients() != null) {
            properties.clients().forEach((name, clientProps) -> {
                clientMap.put(name, buildRestClient(clientProps, sslBundles));
                retryMap.put(name, buildRetryTemplate(clientProps.retry()));
            });
        }

        this.clients = Collections.unmodifiableMap(clientMap);
        this.retryTemplates = Collections.unmodifiableMap(retryMap);
    }

    /**
     * Returns the pre-built {@code RestClient} for the named client.
     *
     * <p>Package-private &mdash; used internally by {@code PlatformRestClient}. Throws {@code NoSuchElementException}
     * when no client with that name is configured.
     *
     * @param clientName the name of the client to retrieve
     * @return the configured {@code RestClient}
     */
    RestClient getClient(String clientName) {
        var client = clients.get(clientName);
        if (client == null) {
            throw new NoSuchElementException("No REST client configured with name: '" + clientName + "'");
        }
        return client;
    }

    /**
     * Returns the {@code RetryTemplate} for the named client.
     *
     * <p>Package-private &mdash; used internally by {@code PlatformRestClient} via {@code executeWithRetry}. When no
     * retry configuration is present for the client, a single-attempt {@code RetryTemplate} (effectively no retry) is
     * returned. Throws {@code NoSuchElementException} when no client with that name is configured.
     *
     * @param clientName the name of the client to retrieve
     * @return the configured {@code RetryTemplate}
     */
    RetryTemplate getRetryTemplate(String clientName) {
        var template = retryTemplates.get(clientName);
        if (template == null) {
            throw new NoSuchElementException("No retry template available for client: '" + clientName + "'");
        }
        return template;
    }

    /**
     * Returns the {@code EndpointProperties} for the named endpoint on the named client.
     *
     * <p>Package-private &mdash; used internally by {@code PlatformRestClient} to resolve endpoint config. Throws
     * {@code NoSuchElementException} when either the client or the endpoint is not configured.
     *
     * @param clientName the name of the client
     * @param endpointName the name of the endpoint
     * @return the configured {@code EndpointProperties}
     */
    EndpointProperties getEndpoint(String clientName, String endpointName) {
        var clientProps = properties.clients() != null ? properties.clients().get(clientName) : null;
        if (clientProps == null) {
            throw new NoSuchElementException("No REST client configured with name: '" + clientName + "'");
        }
        var endpoint = clientProps.endpoints() != null ? clientProps.endpoints().get(endpointName) : null;
        if (endpoint == null) {
            throw new NoSuchElementException(
                    "No endpoint '" + endpointName + "' configured for client: '" + clientName + "'");
        }
        return endpoint;
    }

    /**
     * Executes the given {@code RetryCallback} using the {@code RetryTemplate} of the named client.
     *
     * <p>Package-private &mdash; called by {@code PlatformRestClient} to wrap every HTTP call in the client's retry
     * policy. When the client is configured with {@code retry.retry-on-response-statuses}, the {@code RestClient} built
     * for that client automatically throws {@code PlatformHttpStatusRetryException} for matching status codes, causing
     * the {@code RetryTemplate} to re-execute the callback.
     *
     * <p>Throws {@code NoSuchElementException} when the named client is not configured.
     *
     * @param clientName the name of the client
     * @param callback the callback containing the REST call to execute
     * @param <T> the return type of the callback
     * @param <E> the type of exception thrown by the callback
     * @return the result of the executed callback
     * @throws E if the callback throws an exception
     */
    <T, E extends Exception> T executeWithRetry(String clientName, RetryCallback<T, E> callback) throws E {
        return getRetryTemplate(clientName).execute(callback);
    }

    /**
     * Returns a {@code PlatformRestClient} scoped to the named client.
     *
     * <p>The returned client provides endpoint-aware typed methods that automatically apply the client's retry policy,
     * endpoint-level headers, and query parameters.
     *
     * <p>Throws {@code NoSuchElementException} when no client with that name is configured.
     *
     * @param clientName the name of the client
     * @return a new {@code PlatformRestClient} instance
     */
    public PlatformRestClient getPlatformRestClient(String clientName) {
        if (!clients.containsKey(clientName)) {
            throw new NoSuchElementException("No REST client configured with name: '" + clientName + "'");
        }
        return new PlatformRestClient(clientName, this);
    }

    /**
     * Returns an unmodifiable set of all configured client names.
     *
     * @return the set of client names
     */
    public Set<String> getClientNames() {
        return clients.keySet();
    }

    /**
     * Returns the effective {@code LoggingProperties} for the named endpoint on the named client.
     *
     * <p>The result is produced by merging the client-level {@code logging} settings with the endpoint-level
     * {@code logging} settings, where non-{@code null} endpoint fields take precedence. Returns {@code null} when
     * neither the client nor the endpoint has logging configured.
     *
     * <p>Package-private &mdash; used by {@code PlatformRestClient} to set the logging context attribute on each
     * request.
     *
     * @param clientName the name of the client
     * @param endpointName the name of the endpoint
     * @return the merged {@code LoggingProperties}, or {@code null}
     */
    LoggingProperties getEffectiveLogging(String clientName, String endpointName) {
        var clientProps = properties.clients() != null ? properties.clients().get(clientName) : null;
        if (clientProps == null) {
            return null;
        }
        var endpointProps =
                clientProps.endpoints() != null ? clientProps.endpoints().get(endpointName) : null;
        var endpointLogging = endpointProps != null ? endpointProps.logging() : null;
        return LoggingProperties.merge(clientProps.logging(), endpointLogging);
    }

    // -------------------------------------------------------------------------
    // Internal builders
    // -------------------------------------------------------------------------

    private RestClient buildRestClient(ClientProperties props, SslBundles sslBundles) {
        var builder = RestClient.builder();

        if (props.baseUrl() != null) {
            builder.baseUrl(props.baseUrl());
        }
        if (props.defaultContentType() != null) {
            builder.defaultHeader(HttpHeaders.CONTENT_TYPE, props.defaultContentType());
        }
        if (props.defaultAccept() != null) {
            builder.defaultHeader(HttpHeaders.ACCEPT, props.defaultAccept());
        }
        if (props.defaultQueryParams() != null && !props.defaultQueryParams().isEmpty()) {
            builder.requestInterceptor(new DefaultQueryParamsInterceptor(props.defaultQueryParams()));
        }

        builder.requestInterceptor(new RestClientLoggingInterceptor());
        builder.requestFactory(buildRequestFactory(props, sslBundles));

        // Status handler that converts retryable status codes into PlatformHttpStatusRetryException
        if (props.retry() != null
                && props.retry().retryOnResponseStatuses() != null
                && !props.retry().retryOnResponseStatuses().isEmpty()) {
            var retryStatuses = props.retry().retryOnResponseStatuses();
            builder.defaultStatusHandler(
                    httpStatusCode -> retryStatuses.contains(httpStatusCode.value()), (request, response) -> {
                        throw new PlatformHttpStatusRetryException(
                                response.getStatusCode().value(), request.getURI());
                    });
        }

        return builder.build();
    }

    private ClientHttpRequestFactory buildRequestFactory(ClientProperties props, SslBundles sslBundles) {
        var hasSsl = props.ssl() != null && props.ssl().bundle() != null && sslBundles != null;

        if (hasSsl) {
            var sslBundle = sslBundles.getBundle(props.ssl().bundle());
            var sslContext = sslBundle.createSslContext();
            var httpClientBuilder = HttpClient.newBuilder().sslContext(sslContext);
            if (props.connectTimeout() != null) {
                httpClientBuilder.connectTimeout(props.connectTimeout());
            }
            var factory = new JdkClientHttpRequestFactory(httpClientBuilder.build());
            if (props.readTimeout() != null) {
                factory.setReadTimeout(props.readTimeout());
            }
            return factory;
        }

        // No SSL — use the JDK HttpClient factory, which supports all HTTP methods including PATCH
        var httpClientBuilder = HttpClient.newBuilder();
        if (props.connectTimeout() != null) {
            httpClientBuilder.connectTimeout(props.connectTimeout());
        }
        var factory = new JdkClientHttpRequestFactory(httpClientBuilder.build());
        if (props.readTimeout() != null) {
            factory.setReadTimeout(props.readTimeout());
        }
        return factory;
    }

    private RetryTemplate buildRetryTemplate(RetryProperties retryProps) {
        if (retryProps == null) {
            // No retry config — execute exactly once, never retry
            return RetryTemplate.builder().maxAttempts(1).build();
        }

        int maxAttempts = retryProps.maxAttempts() != null ? retryProps.maxAttempts() : 3;
        var builder = RetryTemplate.builder().maxAttempts(maxAttempts);

        if (retryProps.retryOnResponseStatuses() != null
                && !retryProps.retryOnResponseStatuses().isEmpty()) {
            builder.retryOn(PlatformHttpStatusRetryException.class);
        }

        if (retryProps.waitDuration() != null) {
            long waitMs = retryProps.waitDuration().toMillis();
            double multiplier = retryProps.multiplier() != null ? retryProps.multiplier() : 1.0;
            if (multiplier > 1.0) {
                long maxWaitMs = retryProps.maxWaitDuration() != null
                        ? retryProps.maxWaitDuration().toMillis()
                        : waitMs * 10;
                builder.exponentialBackoff(waitMs, multiplier, maxWaitMs);
            } else {
                builder.fixedBackoff(waitMs);
            }
        }

        return builder.build();
    }

    // -------------------------------------------------------------------------
    // Private interceptor
    // -------------------------------------------------------------------------

    /**
     * {@code ClientHttpRequestInterceptor} that appends a fixed set of query parameters to every outbound request URI.
     */
    private static final class DefaultQueryParamsInterceptor implements ClientHttpRequestInterceptor {

        private final Map<String, String> queryParams;

        private DefaultQueryParamsInterceptor(Map<String, String> queryParams) {
            this.queryParams = queryParams;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException {
            var uriBuilder = UriComponentsBuilder.fromUri(request.getURI());
            queryParams.forEach(uriBuilder::queryParam);
            var modifiedUri = uriBuilder.build(true).toUri();
            return execution.execute(new UriOverridingHttpRequest(request, modifiedUri), body);
        }
    }

    /**
     * Minimal {@code HttpRequest} wrapper that substitutes a different URI while delegating all other methods to the
     * original request.
     */
    private static final class UriOverridingHttpRequest implements HttpRequest {

        private final HttpRequest delegate;
        private final URI uri;

        private UriOverridingHttpRequest(HttpRequest delegate, URI uri) {
            this.delegate = delegate;
            this.uri = uri;
        }

        @Override
        public URI getURI() {
            return uri;
        }

        @Override
        public HttpMethod getMethod() {
            return delegate.getMethod();
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return delegate.getAttributes();
        }
    }
}
