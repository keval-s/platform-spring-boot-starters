package com.kevshah.platform.starter.rest.client;

import com.kevshah.platform.starter.rest.client.config.ClientProperties;
import com.kevshah.platform.starter.rest.client.config.EndpointProperties;
import com.kevshah.platform.starter.rest.client.config.RestClientProperties;
import com.kevshah.platform.starter.rest.client.config.RetryProperties;
import com.kevshah.platform.starter.rest.client.exception.PlatformHttpStatusRetryException;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/// Central registry that holds all pre-configured `RestClient` instances and their
/// corresponding `RetryTemplate`s.
///
/// One `RestClient` and one `RetryTemplate` are created per named client declared under
/// `platform.rest.client.clients.*`. Consumers inject this bean by type and obtain a
/// `PlatformRestClient` scoped to a specific named client:
///
/// ```java
/// @Autowired PlatformRestClientRegistry registry;
///
/// PlatformRestClient paymentClient = registry.getPlatformRestClient("payment-service");
///
/// var result = paymentClient.post("create-payment", request, PaymentResponse.class);
/// ```
///
/// The bean is registered under the name `platformRestClientRegistry` to avoid
/// collisions with any other beans in the application context.
public final class PlatformRestClientRegistry {

    private final Map<String, RestClient> clients;
    private final Map<String, RetryTemplate> retryTemplates;
    private final RestClientProperties properties;

    /// Constructs the registry and eagerly builds all configured clients.
    ///
    /// - `properties` — the bound `@ConfigurationProperties` record.
    /// - `sslBundles` — optional; when `null` the JVM's default SSL context is used for all clients.
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

    /// Returns the pre-built `RestClient` for the named client.
    ///
    /// Package-private — used internally by `PlatformRestClient`.
    /// Throws `NoSuchElementException` when no client with that name is configured.
    RestClient getClient(String clientName) {
        var client = clients.get(clientName);
        if (client == null) {
            throw new NoSuchElementException("No REST client configured with name: '" + clientName + "'");
        }
        return client;
    }

    /// Returns the `RetryTemplate` for the named client.
    ///
    /// Package-private — used internally by `PlatformRestClient` via `executeWithRetry`.
    /// When no retry configuration is present for the client, a single-attempt
    /// `RetryTemplate` (effectively no retry) is returned.
    /// Throws `NoSuchElementException` when no client with that name is configured.
    RetryTemplate getRetryTemplate(String clientName) {
        var template = retryTemplates.get(clientName);
        if (template == null) {
            throw new NoSuchElementException("No retry template available for client: '" + clientName + "'");
        }
        return template;
    }

    /// Returns the `EndpointProperties` for the named endpoint on the named client.
    ///
    /// Package-private — used internally by `PlatformRestClient` to resolve endpoint config.
    /// Throws `NoSuchElementException` when either the client or the endpoint is not configured.
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

    /// Executes the given `RetryCallback` using the `RetryTemplate` of the named client.
    ///
    /// Package-private — called by `PlatformRestClient` to wrap every HTTP call in the
    /// client's retry policy.
    /// When the client is configured with `retry.retry-on-response-statuses`, the
    /// `RestClient` built for that client automatically throws
    /// `PlatformHttpStatusRetryException` for matching status codes, causing the
    /// `RetryTemplate` to re-execute the callback.
    ///
    /// Throws `NoSuchElementException` when the named client is not configured.
    <T, E extends Exception> T executeWithRetry(String clientName,
                                                       RetryCallback<T, E> callback) throws E {
        return getRetryTemplate(clientName).execute(callback);
    }

    /// Returns a `PlatformRestClient` scoped to the named client.
    ///
    /// The returned client provides endpoint-aware typed methods that automatically
    /// apply the client's retry policy, endpoint-level headers, and query parameters.
    ///
    /// Throws `NoSuchElementException` when no client with that name is configured.
    public PlatformRestClient getPlatformRestClient(String clientName) {
        if (!clients.containsKey(clientName)) {
            throw new NoSuchElementException("No REST client configured with name: '" + clientName + "'");
        }
        return new PlatformRestClient(clientName, this);
    }

    /// Returns an unmodifiable set of all configured client names.
    public Set<String> getClientNames() {
        return clients.keySet();
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

        builder.requestFactory(buildRequestFactory(props, sslBundles));

        // Status handler that converts retryable status codes into PlatformHttpStatusRetryException
        if (props.retry() != null
                && props.retry().retryOnResponseStatuses() != null
                && !props.retry().retryOnResponseStatuses().isEmpty()) {
            var retryStatuses = props.retry().retryOnResponseStatuses();
            builder.defaultStatusHandler(
                    httpStatusCode -> retryStatuses.contains(httpStatusCode.value()),
                    (request, response) -> {
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
            return RetryTemplate.builder()
                    .maxAttempts(1)
                    .build();
        }

        int maxAttempts = retryProps.maxAttempts() != null ? retryProps.maxAttempts() : 3;
        var builder = RetryTemplate.builder().maxAttempts(maxAttempts);

        if (retryProps.retryOnResponseStatuses() != null && !retryProps.retryOnResponseStatuses().isEmpty()) {
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

    /// `ClientHttpRequestInterceptor` that appends a fixed set of query parameters to
    /// every outbound request URI.
    private static final class DefaultQueryParamsInterceptor implements ClientHttpRequestInterceptor {

        private final Map<String, String> queryParams;

        private DefaultQueryParamsInterceptor(Map<String, String> queryParams) {
            this.queryParams = queryParams;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            var uriBuilder = UriComponentsBuilder.fromUri(request.getURI());
            queryParams.forEach(uriBuilder::queryParam);
            var modifiedUri = uriBuilder.build(true).toUri();
            return execution.execute(new UriOverridingHttpRequest(request, modifiedUri), body);
        }
    }

    /// Minimal `HttpRequest` wrapper that substitutes a different URI while
    /// delegating all other methods to the original request.
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






