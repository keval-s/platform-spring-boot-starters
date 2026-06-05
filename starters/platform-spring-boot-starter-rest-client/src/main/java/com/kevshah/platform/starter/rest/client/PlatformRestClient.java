package com.kevshah.platform.starter.rest.client;

import com.kevshah.platform.starter.rest.client.config.EndpointProperties;
import com.kevshah.platform.starter.rest.client.logging.LoggingContext;
import com.kevshah.platform.starter.rest.client.logging.RestClientLoggingInterceptor;
import java.net.URI;
import java.util.Map;
import java.util.function.Function;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

/**
 * A high-level REST client scoped to a single named client configuration.
 *
 * <p>{@code PlatformRestClient} wraps Spring's {@code RestClient} and provides typed, endpoint-aware methods for
 * invoking pre-configured endpoints. All calls are automatically wrapped in the client's retry policy, and
 * endpoint-level headers and query parameters are applied on every request.
 *
 * <p>Obtain an instance from {@link PlatformRestClientRegistry#getClient}:
 *
 * <p>
 *
 * <pre>{@code
 * PlatformRestClient paymentClient = registry.client("payment-service");
 *
 * // Invoke a configured endpoint — HTTP method is read from the endpoint config
 * var response = paymentClient.exchange("create-payment", request, PaymentResponse.class);
 *
 * // Pass custom per-request headers
 * var headers = new HttpHeaders();
 * headers.set("X-Correlation-Id", correlationId);
 * var payment = paymentClient.get("get-payment", Map.of("id", "pay-123"), headers, PaymentResponse.class);
 * }</pre>
 */
public final class PlatformRestClient {

    private final String clientName;
    private final PlatformRestClientRegistry registry;

    /**
     * Package-private constructor &mdash; instances are created by {@code PlatformRestClientRegistry}.
     *
     * @param clientName the name of the client
     * @param registry the registry managing this client
     */
    PlatformRestClient(String clientName, PlatformRestClientRegistry registry) {
        this.clientName = clientName;
        this.registry = registry;
    }

    /**
     * Returns the name of the underlying client configuration.
     *
     * @return the client name
     */
    public String clientName() {
        return clientName;
    }

    // -------------------------------------------------------------------------
    // exchange — uses the HTTP method configured on the endpoint
    // -------------------------------------------------------------------------

    /**
     * Invokes the named endpoint using its configured HTTP method.
     *
     * <p>URI template variables in the endpoint path (e.g. {@code /api/v1/payments/{id}}) are expanded using
     * {@code uriVariables}. The call is wrapped in the client's retry policy.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T exchange(String endpointName, Object body, Map<String, ?> uriVariables, Class<T> responseType) {
        return doExchange(endpointName, null, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /**
     * Invokes the named endpoint using its configured HTTP method, merging the supplied headers into the request after
     * endpoint-level headers.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T exchange(
            String endpointName, Object body, Map<String, ?> uriVariables, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, null, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    /**
     * Invokes the named endpoint using its configured HTTP method, with no URI variables.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T exchange(String endpointName, Object body, Class<T> responseType) {
        return doExchange(endpointName, null, body, null, null, spec -> spec.body(responseType));
    }

    /**
     * Invokes the named endpoint using its configured HTTP method, with no URI variables, merging the supplied headers
     * into the request after endpoint-level headers.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T exchange(String endpointName, Object body, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, null, body, null, headers, spec -> spec.body(responseType));
    }

    /**
     * Invokes the named endpoint using its configured HTTP method, with no body or URI variables.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T exchange(String endpointName, Class<T> responseType) {
        return doExchange(endpointName, null, null, null, null, spec -> spec.body(responseType));
    }

    /**
     * Invokes the named endpoint using its configured HTTP method, with no body or URI variables, merging the supplied
     * headers into the request after endpoint-level headers.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T exchange(String endpointName, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, null, null, null, headers, spec -> spec.body(responseType));
    }

    /**
     * Invokes the named endpoint using its configured HTTP method.
     *
     * <p>Accepts a {@code ParameterizedTypeReference} to support generic response types such as
     * {@code List<PaymentResponse>}.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T exchange(
            String endpointName, Object body, Map<String, ?> uriVariables, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, null, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /**
     * Invokes the named endpoint using its configured HTTP method, merging the supplied headers into the request after
     * endpoint-level headers.
     *
     * <p>Accepts a {@code ParameterizedTypeReference} to support generic response types such as
     * {@code List<PaymentResponse>}.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T exchange(
            String endpointName,
            Object body,
            Map<String, ?> uriVariables,
            HttpHeaders headers,
            ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, null, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    /**
     * Invokes the named endpoint using its configured HTTP method, with no URI variables.
     *
     * <p>Accepts a {@code ParameterizedTypeReference} to support generic response types.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T exchange(String endpointName, Object body, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, null, body, null, null, spec -> spec.body(responseType));
    }

    /**
     * Invokes the named endpoint using its configured HTTP method, with no URI variables, merging the supplied headers
     * into the request after endpoint-level headers.
     *
     * <p>Accepts a {@code ParameterizedTypeReference} to support generic response types.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T exchange(
            String endpointName, Object body, HttpHeaders headers, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, null, body, null, headers, spec -> spec.body(responseType));
    }

    /**
     * Invokes the named endpoint using its configured HTTP method, with no body or URI variables.
     *
     * <p>Accepts a {@code ParameterizedTypeReference} to support generic response types.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T exchange(String endpointName, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, null, null, null, null, spec -> spec.body(responseType));
    }

    /**
     * Invokes the named endpoint using its configured HTTP method, with no body or URI variables, merging the supplied
     * headers into the request after endpoint-level headers.
     *
     * <p>Accepts a {@code ParameterizedTypeReference} to support generic response types.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T exchange(String endpointName, HttpHeaders headers, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, null, null, null, headers, spec -> spec.body(responseType));
    }

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    /**
     * Performs a GET request to the named endpoint.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T get(String endpointName, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, null, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a GET request to the named endpoint, merging the supplied headers into the request after endpoint-level
     * headers.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T get(String endpointName, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, null, headers, spec -> spec.body(responseType));
    }

    /**
     * Performs a GET request to the named endpoint, expanding URI template variables.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T get(String endpointName, Map<String, ?> uriVariables, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, uriVariables, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a GET request to the named endpoint, expanding URI template variables and merging the supplied headers
     * into the request after endpoint-level headers.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T get(String endpointName, Map<String, ?> uriVariables, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, uriVariables, headers, spec -> spec.body(responseType));
    }

    /**
     * Performs a GET request to the named endpoint with a parameterised response type.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T get(String endpointName, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, null, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a GET request to the named endpoint with a parameterised response type, merging the supplied headers
     * into the request after endpoint-level headers.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T get(String endpointName, HttpHeaders headers, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, null, headers, spec -> spec.body(responseType));
    }

    /**
     * Performs a GET request to the named endpoint, expanding URI template variables, with a parameterised response
     * type.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T get(String endpointName, Map<String, ?> uriVariables, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, uriVariables, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a GET request to the named endpoint, expanding URI template variables and merging the supplied headers
     * into the request after endpoint-level headers, with a parameterised response type.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T get(
            String endpointName,
            Map<String, ?> uriVariables,
            HttpHeaders headers,
            ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, uriVariables, headers, spec -> spec.body(responseType));
    }

    // -------------------------------------------------------------------------
    // POST
    // -------------------------------------------------------------------------

    /**
     * Performs a POST request to the named endpoint with the given request body.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T post(String endpointName, Object body, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, null, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a POST request to the named endpoint with the given request body, merging the supplied headers into the
     * request after endpoint-level headers.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T post(String endpointName, Object body, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, null, headers, spec -> spec.body(responseType));
    }

    /**
     * Performs a POST request to the named endpoint, expanding URI template variables.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T post(String endpointName, Object body, Map<String, ?> uriVariables, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a POST request to the named endpoint, expanding URI template variables and merging the supplied headers
     * into the request after endpoint-level headers.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T post(
            String endpointName, Object body, Map<String, ?> uriVariables, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    /**
     * Performs a POST request to the named endpoint with a parameterised response type.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T post(String endpointName, Object body, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, null, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a POST request to the named endpoint with a parameterised response type, merging the supplied headers
     * into the request after endpoint-level headers.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T post(
            String endpointName, Object body, HttpHeaders headers, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, null, headers, spec -> spec.body(responseType));
    }

    /**
     * Performs a POST request, expanding URI template variables, with a parameterised response type.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T post(
            String endpointName, Object body, Map<String, ?> uriVariables, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a POST request, expanding URI template variables and merging the supplied headers into the request after
     * endpoint-level headers, with a parameterised response type.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T post(
            String endpointName,
            Object body,
            Map<String, ?> uriVariables,
            HttpHeaders headers,
            ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    // -------------------------------------------------------------------------
    // PUT
    // -------------------------------------------------------------------------

    /**
     * Performs a PUT request to the named endpoint with the given request body.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T put(String endpointName, Object body, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, null, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a PUT request to the named endpoint with the given request body, merging the supplied headers into the
     * request after endpoint-level headers.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T put(String endpointName, Object body, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, null, headers, spec -> spec.body(responseType));
    }

    /**
     * Performs a PUT request to the named endpoint, expanding URI template variables.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T put(String endpointName, Object body, Map<String, ?> uriVariables, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a PUT request to the named endpoint, expanding URI template variables and merging the supplied headers
     * into the request after endpoint-level headers.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T put(
            String endpointName, Object body, Map<String, ?> uriVariables, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    /**
     * Performs a PUT request to the named endpoint with a parameterised response type.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T put(String endpointName, Object body, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, null, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a PUT request to the named endpoint with a parameterised response type, merging the supplied headers
     * into the request after endpoint-level headers.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T put(
            String endpointName, Object body, HttpHeaders headers, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, null, headers, spec -> spec.body(responseType));
    }

    /**
     * Performs a PUT request, expanding URI template variables, with a parameterised response type.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T put(
            String endpointName, Object body, Map<String, ?> uriVariables, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a PUT request, expanding URI template variables and merging the supplied headers into the request after
     * endpoint-level headers, with a parameterised response type.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T put(
            String endpointName,
            Object body,
            Map<String, ?> uriVariables,
            HttpHeaders headers,
            ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    // -------------------------------------------------------------------------
    // PATCH
    // -------------------------------------------------------------------------

    /**
     * Performs a PATCH request to the named endpoint with the given request body.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T patch(String endpointName, Object body, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, null, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a PATCH request to the named endpoint with the given request body, merging the supplied headers into the
     * request after endpoint-level headers.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T patch(String endpointName, Object body, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, null, headers, spec -> spec.body(responseType));
    }

    /**
     * Performs a PATCH request to the named endpoint, expanding URI template variables.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T patch(String endpointName, Object body, Map<String, ?> uriVariables, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a PATCH request to the named endpoint, expanding URI template variables and merging the supplied headers
     * into the request after endpoint-level headers.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Class to deserialize the response body into.
     * @return Deserialized response body of type {@code responseType}.
     */
    public <T> T patch(
            String endpointName, Object body, Map<String, ?> uriVariables, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    /**
     * Performs a PATCH request to the named endpoint with a parameterised response type.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T patch(String endpointName, Object body, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, null, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a PATCH request to the named endpoint with a parameterised response type, merging the supplied headers
     * into the request after endpoint-level headers.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T patch(
            String endpointName, Object body, HttpHeaders headers, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, null, headers, spec -> spec.body(responseType));
    }

    /**
     * Performs a PATCH request, expanding URI template variables, with a parameterised response type.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T patch(
            String endpointName, Object body, Map<String, ?> uriVariables, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /**
     * Performs a PATCH request, expanding URI template variables and merging the supplied headers into the request
     * after endpoint-level headers, with a parameterised response type.
     *
     * @param <T> Expected response type. Used for deserialization.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param body Request body to attach to the request.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param headers Call-specific headers, merged after endpoint defaults.
     * @param responseType Generic type reference for response deserialization.
     * @return Deserialized response body of type {@code T}.
     */
    public <T> T patch(
            String endpointName,
            Object body,
            Map<String, ?> uriVariables,
            HttpHeaders headers,
            ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    /**
     * Performs a DELETE request to the named endpoint.
     *
     * @param endpointName Name of the configured endpoint to invoke.
     */
    public void delete(String endpointName) {
        doExchange(endpointName, HttpMethod.DELETE, null, null, null, RestClient.ResponseSpec::toBodilessEntity);
    }

    /**
     * Performs a DELETE request to the named endpoint, merging the supplied headers into the request after
     * endpoint-level headers.
     *
     * @param endpointName Name of the configured endpoint to invoke.
     * @param headers Call-specific headers, merged after endpoint defaults.
     */
    public void delete(String endpointName, HttpHeaders headers) {
        doExchange(endpointName, HttpMethod.DELETE, null, null, headers, RestClient.ResponseSpec::toBodilessEntity);
    }

    /**
     * Performs a DELETE request to the named endpoint, expanding URI template variables.
     *
     * @param endpointName Name of the configured endpoint to invoke.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     */
    public void delete(String endpointName, Map<String, ?> uriVariables) {
        doExchange(
                endpointName, HttpMethod.DELETE, null, uriVariables, null, RestClient.ResponseSpec::toBodilessEntity);
    }

    /**
     * Performs a DELETE request to the named endpoint, expanding URI template variables and merging the supplied
     * headers into the request after endpoint-level headers.
     *
     * @param endpointName Name of the configured endpoint to invoke.
     * @param uriVariables Map of URI variables to expand the endpoint path.
     * @param headers Call-specific headers, merged after endpoint defaults.
     */
    public void delete(String endpointName, Map<String, ?> uriVariables, HttpHeaders headers) {
        doExchange(
                endpointName,
                HttpMethod.DELETE,
                null,
                uriVariables,
                headers,
                RestClient.ResponseSpec::toBodilessEntity);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    /**
     * Core execution method that all public HTTP methods delegate to.
     *
     * <p>Builds the request from the endpoint configuration, applies endpoint-level headers, then merges any
     * caller-supplied {@code additionalHeaders}, attaches an optional body, and passes the {@code ResponseSpec} to the
     * {@code responseExtractor} function. The whole call is executed inside the client's {@code RetryTemplate}.
     *
     * @param <T> Type of the extracted response value.
     * @param endpointName Name of the configured endpoint to invoke.
     * @param methodOverride Optional HTTP method overriding the endpoint default.
     * @param body Optional request body to attach (null if unsupported).
     * @param uriVariables Optional URI variables to expand the endpoint path.
     * @param additionalHeaders Optional extra headers merging after endpoint defaults.
     * @param responseExtractor Function extracting the desired return value from ResponseSpec.
     * @return The value returned by {@code responseExtractor}, typically the deserialized response body.
     */
    private <T> T doExchange(
            String endpointName,
            HttpMethod methodOverride,
            Object body,
            Map<String, ?> uriVariables,
            HttpHeaders additionalHeaders,
            Function<RestClient.ResponseSpec, T> responseExtractor) {
        var endpoint = registry.getEndpoint(clientName, endpointName);
        var restClient = registry.getClient(clientName);
        var httpMethod = methodOverride != null ? methodOverride : HttpMethod.valueOf(endpoint.method());
        var effectiveLogging = registry.getEffectiveLogging(clientName, endpointName);

        return registry.executeWithRetry(clientName, ctx -> {
            var requestSpec = restClient.method(httpMethod).uri(buildUri(endpoint, uriVariables));

            applyEndpointHeaders(requestSpec, endpoint);
            applyAdditionalHeaders(requestSpec, additionalHeaders);

            if (effectiveLogging != null && Boolean.TRUE.equals(effectiveLogging.enabled())) {
                requestSpec.attribute(
                        RestClientLoggingInterceptor.ATTRIBUTE_KEY,
                        new LoggingContext(clientName, endpointName, effectiveLogging));
            }

            var responseSpec = body != null ? requestSpec.body(body).retrieve() : requestSpec.retrieve();

            return responseExtractor.apply(responseSpec);
        });
    }

    /**
     * Builds a URI builder function that combines the endpoint path, endpoint-level default query parameters, and any
     * caller-supplied URI template variables.
     *
     * @param endpoint the endpoint configuration
     * @param uriVariables map of URI template variables (optional)
     * @return a function that builds the final {@code URI}
     */
    private static Function<UriBuilder, URI> buildUri(EndpointProperties endpoint, Map<String, ?> uriVariables) {
        return builder -> {
            var b = builder.path(endpoint.path());
            if (endpoint.defaultQueryParams() != null
                    && !endpoint.defaultQueryParams().isEmpty()) {
                endpoint.defaultQueryParams().forEach(b::queryParam);
            }
            if (uriVariables != null && !uriVariables.isEmpty()) {
                return b.build(uriVariables);
            }
            return b.build();
        };
    }

    /**
     * Applies endpoint-level {@code Content-Type} and {@code Accept} header overrides to the request.
     *
     * <p>When set on the endpoint, these take precedence over any client-level defaults.
     *
     * @param requestSpec the request spec to modify
     * @param endpoint the endpoint configuration containing header overrides
     */
    private static void applyEndpointHeaders(RestClient.RequestBodySpec requestSpec, EndpointProperties endpoint) {
        if (endpoint.contentType() != null) {
            requestSpec.header(HttpHeaders.CONTENT_TYPE, endpoint.contentType());
        }
        if (endpoint.accept() != null) {
            requestSpec.header(HttpHeaders.ACCEPT, endpoint.accept());
        }
    }

    /**
     * Merges caller-supplied headers into the request, adding each header value to the request.
     *
     * <p>This is a no-op when {@code headers} is {@code null} or empty.
     *
     * @param requestSpec The request spec to apply headers to.
     * @param headers Caller-supplied headers to merge; may be null.
     */
    private static void applyAdditionalHeaders(RestClient.RequestBodySpec requestSpec, HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        headers.forEach((name, values) -> values.forEach(value -> requestSpec.header(name, value)));
    }
}
