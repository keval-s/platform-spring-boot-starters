package com.kevshah.platform.starter.rest.client;

import com.kevshah.platform.starter.rest.client.config.EndpointProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;

/// A high-level REST client scoped to a single named client configuration.
///
/// `PlatformRestClient` wraps Spring's `RestClient` and provides typed, endpoint-aware
/// methods for invoking pre-configured endpoints. All calls are automatically wrapped
/// in the client's retry policy, and endpoint-level headers and query parameters are
/// applied on every request.
///
/// Obtain an instance from [PlatformRestClientRegistry#getClient]:
///
/// ```java
/// PlatformRestClient paymentClient = registry.client("payment-service");
///
/// // Invoke a configured endpoint — HTTP method is read from the endpoint config
/// var response = paymentClient.exchange("create-payment", request, PaymentResponse.class);
///
/// // Pass custom per-request headers
/// var headers = new HttpHeaders();
/// headers.set("X-Correlation-Id", correlationId);
/// var payment = paymentClient.get("get-payment", Map.of("id", "pay-123"), headers, PaymentResponse.class);
/// ```
public final class PlatformRestClient {

    private final String clientName;
    private final PlatformRestClientRegistry registry;

    /// Package-private constructor — instances are created by `PlatformRestClientRegistry`.
    PlatformRestClient(String clientName, PlatformRestClientRegistry registry) {
        this.clientName = clientName;
        this.registry = registry;
    }

    /// Returns the name of the underlying client configuration.
    public String clientName() {
        return clientName;
    }

    // -------------------------------------------------------------------------
    // exchange — uses the HTTP method configured on the endpoint
    // -------------------------------------------------------------------------

    /// Invokes the named endpoint using its configured HTTP method.
    ///
    /// URI template variables in the endpoint path (e.g. `/api/v1/payments/{id}`) are
    /// expanded using `uriVariables`. The call is wrapped in the client's retry policy.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T exchange(String endpointName, Object body, Map<String, ?> uriVariables,
                          Class<T> responseType) {
        return doExchange(endpointName, null, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /// Invokes the named endpoint using its configured HTTP method, merging the supplied headers
    /// into the request after endpoint-level headers.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T exchange(String endpointName, Object body, Map<String, ?> uriVariables,
                          HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, null, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    /// Invokes the named endpoint using its configured HTTP method, with no URI variables.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the request.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T exchange(String endpointName, Object body, Class<T> responseType) {
        return doExchange(endpointName, null, body, null, null, spec -> spec.body(responseType));
    }

    /// Invokes the named endpoint using its configured HTTP method, with no URI variables,
    /// merging the supplied headers into the request after endpoint-level headers.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the request.
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T exchange(String endpointName, Object body, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, null, body, null, headers, spec -> spec.body(responseType));
    }

    /// Invokes the named endpoint using its configured HTTP method, with no body or URI variables.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T exchange(String endpointName, Class<T> responseType) {
        return doExchange(endpointName, null, null, null, null, spec -> spec.body(responseType));
    }

    /// Invokes the named endpoint using its configured HTTP method, with no body or URI variables,
    /// merging the supplied headers into the request after endpoint-level headers.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T exchange(String endpointName, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, null, null, null, headers, spec -> spec.body(responseType));
    }

    /// Invokes the named endpoint using its configured HTTP method.
    ///
    /// Accepts a `ParameterizedTypeReference` to support generic response types
    /// such as `List<PaymentResponse>`.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T exchange(String endpointName, Object body, Map<String, ?> uriVariables,
                          ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, null, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /// Invokes the named endpoint using its configured HTTP method, merging the supplied headers
    /// into the request after endpoint-level headers.
    ///
    /// Accepts a `ParameterizedTypeReference` to support generic response types
    /// such as `List<PaymentResponse>`.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T exchange(String endpointName, Object body, Map<String, ?> uriVariables,
                          HttpHeaders headers, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, null, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    /// Invokes the named endpoint using its configured HTTP method, with no URI variables.
    ///
    /// Accepts a `ParameterizedTypeReference` to support generic response types.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the request.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T exchange(String endpointName, Object body,
                          ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, null, body, null, null, spec -> spec.body(responseType));
    }

    /// Invokes the named endpoint using its configured HTTP method, with no URI variables,
    /// merging the supplied headers into the request after endpoint-level headers.
    ///
    /// Accepts a `ParameterizedTypeReference` to support generic response types.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the request.
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T exchange(String endpointName, Object body, HttpHeaders headers,
                          ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, null, body, null, headers, spec -> spec.body(responseType));
    }

    /// Invokes the named endpoint using its configured HTTP method, with no body or URI variables.
    ///
    /// Accepts a `ParameterizedTypeReference` to support generic response types.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T exchange(String endpointName, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, null, null, null, null, spec -> spec.body(responseType));
    }

    /// Invokes the named endpoint using its configured HTTP method, with no body or URI variables,
    /// merging the supplied headers into the request after endpoint-level headers.
    ///
    /// Accepts a `ParameterizedTypeReference` to support generic response types.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T exchange(String endpointName, HttpHeaders headers,
                          ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, null, null, null, headers, spec -> spec.body(responseType));
    }

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    /// Performs a GET request to the named endpoint.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T get(String endpointName, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, null, null, spec -> spec.body(responseType));
    }

    /// Performs a GET request to the named endpoint, merging the supplied headers into the
    /// request after endpoint-level headers.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T get(String endpointName, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, null, headers, spec -> spec.body(responseType));
    }

    /// Performs a GET request to the named endpoint, expanding URI template variables.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T get(String endpointName, Map<String, ?> uriVariables, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, uriVariables, null, spec -> spec.body(responseType));
    }

    /// Performs a GET request to the named endpoint, expanding URI template variables and
    /// merging the supplied headers into the request after endpoint-level headers.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T get(String endpointName, Map<String, ?> uriVariables, HttpHeaders headers,
                     Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, uriVariables, headers, spec -> spec.body(responseType));
    }

    /// Performs a GET request to the named endpoint with a parameterised response type.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T get(String endpointName, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, null, null, spec -> spec.body(responseType));
    }

    /// Performs a GET request to the named endpoint with a parameterised response type,
    /// merging the supplied headers into the request after endpoint-level headers.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T get(String endpointName, HttpHeaders headers,
                     ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, null, headers, spec -> spec.body(responseType));
    }

    /// Performs a GET request to the named endpoint, expanding URI template variables,
    /// with a parameterised response type.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T get(String endpointName, Map<String, ?> uriVariables,
                     ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, uriVariables, null, spec -> spec.body(responseType));
    }

    /// Performs a GET request to the named endpoint, expanding URI template variables and
    /// merging the supplied headers into the request after endpoint-level headers,
    /// with a parameterised response type.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T get(String endpointName, Map<String, ?> uriVariables, HttpHeaders headers,
                     ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.GET, null, uriVariables, headers, spec -> spec.body(responseType));
    }

    // -------------------------------------------------------------------------
    // POST
    // -------------------------------------------------------------------------

    /// Performs a POST request to the named endpoint with the given request body.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the POST request.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T post(String endpointName, Object body, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, null, null, spec -> spec.body(responseType));
    }

    /// Performs a POST request to the named endpoint with the given request body, merging
    /// the supplied headers into the request after endpoint-level headers.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the POST request.
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T post(String endpointName, Object body, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, null, headers, spec -> spec.body(responseType));
    }

    /// Performs a POST request to the named endpoint, expanding URI template variables.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the POST request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T post(String endpointName, Object body, Map<String, ?> uriVariables, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /// Performs a POST request to the named endpoint, expanding URI template variables and
    /// merging the supplied headers into the request after endpoint-level headers.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the POST request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T post(String endpointName, Object body, Map<String, ?> uriVariables,
                      HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    /// Performs a POST request to the named endpoint with a parameterised response type.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the POST request.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T post(String endpointName, Object body, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, null, null, spec -> spec.body(responseType));
    }

    /// Performs a POST request to the named endpoint with a parameterised response type,
    /// merging the supplied headers into the request after endpoint-level headers.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the POST request.
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T post(String endpointName, Object body, HttpHeaders headers,
                      ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, null, headers, spec -> spec.body(responseType));
    }

    /// Performs a POST request, expanding URI template variables, with a parameterised response type.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the POST request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T post(String endpointName, Object body, Map<String, ?> uriVariables,
                      ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /// Performs a POST request, expanding URI template variables and merging the supplied
    /// headers into the request after endpoint-level headers, with a parameterised response type.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the POST request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T post(String endpointName, Object body, Map<String, ?> uriVariables,
                      HttpHeaders headers, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.POST, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    // -------------------------------------------------------------------------
    // PUT
    // -------------------------------------------------------------------------

    /// Performs a PUT request to the named endpoint with the given request body.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PUT request.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T put(String endpointName, Object body, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, null, null, spec -> spec.body(responseType));
    }

    /// Performs a PUT request to the named endpoint with the given request body, merging
    /// the supplied headers into the request after endpoint-level headers.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PUT request.
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T put(String endpointName, Object body, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, null, headers, spec -> spec.body(responseType));
    }

    /// Performs a PUT request to the named endpoint, expanding URI template variables.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PUT request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T put(String endpointName, Object body, Map<String, ?> uriVariables, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /// Performs a PUT request to the named endpoint, expanding URI template variables and
    /// merging the supplied headers into the request after endpoint-level headers.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PUT request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T put(String endpointName, Object body, Map<String, ?> uriVariables,
                     HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    /// Performs a PUT request to the named endpoint with a parameterised response type.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PUT request.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T put(String endpointName, Object body, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, null, null, spec -> spec.body(responseType));
    }

    /// Performs a PUT request to the named endpoint with a parameterised response type,
    /// merging the supplied headers into the request after endpoint-level headers.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PUT request.
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T put(String endpointName, Object body, HttpHeaders headers,
                     ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, null, headers, spec -> spec.body(responseType));
    }

    /// Performs a PUT request, expanding URI template variables, with a parameterised response type.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PUT request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T put(String endpointName, Object body, Map<String, ?> uriVariables,
                     ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /// Performs a PUT request, expanding URI template variables and merging the supplied
    /// headers into the request after endpoint-level headers, with a parameterised response type.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PUT request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T put(String endpointName, Object body, Map<String, ?> uriVariables,
                     HttpHeaders headers, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PUT, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    // -------------------------------------------------------------------------
    // PATCH
    // -------------------------------------------------------------------------

    /// Performs a PATCH request to the named endpoint with the given request body.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PATCH request.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T patch(String endpointName, Object body, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, null, null, spec -> spec.body(responseType));
    }

    /// Performs a PATCH request to the named endpoint with the given request body, merging
    /// the supplied headers into the request after endpoint-level headers.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PATCH request.
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T patch(String endpointName, Object body, HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, null, headers, spec -> spec.body(responseType));
    }

    /// Performs a PATCH request to the named endpoint, expanding URI template variables.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PATCH request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T patch(String endpointName, Object body, Map<String, ?> uriVariables, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /// Performs a PATCH request to the named endpoint, expanding URI template variables and
    /// merging the supplied headers into the request after endpoint-level headers.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PATCH request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType Class of the expected response body type. The response body is deserialized into an instance of this class and returned.
    /// @return Deserialized response body of type `responseType`.
    public <T> T patch(String endpointName, Object body, Map<String, ?> uriVariables,
                       HttpHeaders headers, Class<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    /// Performs a PATCH request to the named endpoint with a parameterised response type.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PATCH request.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T patch(String endpointName, Object body, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, null, null, spec -> spec.body(responseType));
    }

    /// Performs a PATCH request to the named endpoint with a parameterised response type,
    /// merging the supplied headers into the request after endpoint-level headers.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PATCH request.
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T patch(String endpointName, Object body, HttpHeaders headers,
                       ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, null, headers, spec -> spec.body(responseType));
    }

    /// Performs a PATCH request, expanding URI template variables, with a parameterised response type.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PATCH request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T patch(String endpointName, Object body, Map<String, ?> uriVariables,
                       ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, uriVariables, null, spec -> spec.body(responseType));
    }

    /// Performs a PATCH request, expanding URI template variables and merging the supplied
    /// headers into the request after endpoint-level headers, with a parameterised response type.
    ///
    /// @param <T>          Type of the expected response body, used for deserialization. This can be a generic type such as `List<PaymentResponse>`, in which case `responseType` should be a `ParameterizedTypeReference<T>`.
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param body         Request body to attach to the PATCH request.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    /// @param responseType ParameterizedTypeReference describing the expected response body type. This allows for deserialization of generic types (e.g. `new ParameterizedTypeReference<List<PaymentResponse>>() {}`).
    /// @return Deserialized response body of type `T`.
    public <T> T patch(String endpointName, Object body, Map<String, ?> uriVariables,
                       HttpHeaders headers, ParameterizedTypeReference<T> responseType) {
        return doExchange(endpointName, HttpMethod.PATCH, body, uriVariables, headers, spec -> spec.body(responseType));
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    /// Performs a DELETE request to the named endpoint.
    ///
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    public void delete(String endpointName) {
        doExchange(endpointName, HttpMethod.DELETE, null, null, null,
                RestClient.ResponseSpec::toBodilessEntity);
    }

    /// Performs a DELETE request to the named endpoint, merging the supplied headers into
    /// the request after endpoint-level headers.
    ///
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    public void delete(String endpointName, HttpHeaders headers) {
        doExchange(endpointName, HttpMethod.DELETE, null, null, headers,
                RestClient.ResponseSpec::toBodilessEntity);
    }

    /// Performs a DELETE request to the named endpoint, expanding URI template variables.
    ///
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    public void delete(String endpointName, Map<String, ?> uriVariables) {
        doExchange(endpointName, HttpMethod.DELETE, null, uriVariables, null,
                RestClient.ResponseSpec::toBodilessEntity);
    }

    /// Performs a DELETE request to the named endpoint, expanding URI template variables and
    /// merging the supplied headers into the request after endpoint-level headers.
    ///
    /// @param endpointName Name of the endpoint to invoke, as defined in the client configuration.
    /// @param uriVariables Map of URI template variable names to values for expanding the endpoint path (e.g. `id` -> `pay-123` for an endpoint path like `/api/v1/payments/{id}`).
    /// @param headers      Additional request headers to include on this call; these are merged after endpoint-level header defaults.
    public void delete(String endpointName, Map<String, ?> uriVariables, HttpHeaders headers) {
        doExchange(endpointName, HttpMethod.DELETE, null, uriVariables, headers,
                RestClient.ResponseSpec::toBodilessEntity);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    /// Core execution method that all public HTTP methods delegate to.
    ///
    /// Builds the request from the endpoint configuration, applies endpoint-level headers,
    /// then merges any caller-supplied `additionalHeaders`, attaches an optional body, and
    /// passes the `ResponseSpec` to the `responseExtractor` function. The whole call is
    /// executed inside the client's `RetryTemplate`.
    ///
    /// @param <T>               Type of the value returned by `responseExtractor`, typically the deserialized response body.
    /// @param endpointName      Name of the endpoint to invoke.
    /// @param methodOverride    Optional HTTP method to use instead of the one configured on the endpoint.
    ///                          This allows the public HTTP-method-specific helpers (e.g. `get()`, `post()`) to delegate to this core
    ///                          method while still respecting endpoint-level configuration when `methodOverride` is `null`.
    /// @param body              Optional request body to attach. May be `null` for methods that don't support bodies (e.g. GET, DELETE).
    /// @param uriVariables      Optional URI template variables to expand in the endpoint path. May be `null` if the endpoint path contains
    ///                          no variables.
    /// @param additionalHeaders Optional caller-supplied headers to merge into the request after endpoint-level header defaults.
    ///                          May be `null` when no extra headers are needed.
    /// @param responseExtractor Function that takes the `ResponseSpec` after the request is sent and extracts the desired return value
    ///                           (e.g. by calling `body(Class<T>)` or `body(ParameterizedTypeReference<T>)`).
    /// @return The value returned by `responseExtractor`, typically the deserialized response body.
    private <T> T doExchange(String endpointName, HttpMethod methodOverride, Object body,
                             Map<String, ?> uriVariables, HttpHeaders additionalHeaders,
                             Function<RestClient.ResponseSpec, T> responseExtractor) {
        var endpoint = registry.getEndpoint(clientName, endpointName);
        var restClient = registry.getClient(clientName);
        var httpMethod = methodOverride != null ? methodOverride : HttpMethod.valueOf(endpoint.method());

        return registry.executeWithRetry(clientName, ctx -> {
            var requestSpec = restClient.method(httpMethod)
                    .uri(buildUri(endpoint, uriVariables));

            applyEndpointHeaders(requestSpec, endpoint);
            applyAdditionalHeaders(requestSpec, additionalHeaders);

            var responseSpec = body != null
                    ? requestSpec.body(body).retrieve()
                    : requestSpec.retrieve();

            return responseExtractor.apply(responseSpec);
        });
    }

    /// Builds a URI builder function that combines the endpoint path, endpoint-level
    /// default query parameters, and any caller-supplied URI template variables.
    private static Function<UriBuilder, URI> buildUri(EndpointProperties endpoint,
                                                      Map<String, ?> uriVariables) {
        return builder -> {
            var b = builder.path(endpoint.path());
            if (endpoint.defaultQueryParams() != null && !endpoint.defaultQueryParams().isEmpty()) {
                endpoint.defaultQueryParams().forEach(b::queryParam);
            }
            if (uriVariables != null && !uriVariables.isEmpty()) {
                return b.build(uriVariables);
            }
            return b.build();
        };
    }

    /// Applies endpoint-level `Content-Type` and `Accept` header overrides to the request.
    ///
    /// When set on the endpoint, these take precedence over any client-level defaults.
    private static void applyEndpointHeaders(RestClient.RequestBodySpec requestSpec,
                                             EndpointProperties endpoint) {
        if (endpoint.contentType() != null) {
            requestSpec.header(HttpHeaders.CONTENT_TYPE, endpoint.contentType());
        }
        if (endpoint.accept() != null) {
            requestSpec.header(HttpHeaders.ACCEPT, endpoint.accept());
        }
    }

    /// Merges caller-supplied headers into the request, adding each header value to the request.
    ///
    /// This is a no-op when `headers` is `null` or empty.
    ///
    /// @param requestSpec the request spec to apply headers to
    /// @param headers     the caller-supplied headers to merge; may be `null`
    private static void applyAdditionalHeaders(RestClient.RequestBodySpec requestSpec,
                                               HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        headers.forEach((name, values) -> values.forEach(value -> requestSpec.header(name, value)));
    }
}
