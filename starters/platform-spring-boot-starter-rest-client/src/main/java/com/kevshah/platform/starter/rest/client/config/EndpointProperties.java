package com.kevshah.platform.starter.rest.client.config;

import java.util.Map;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration for a single named endpoint on a REST client.
 *
 * <p>An endpoint defines the HTTP method, URL path, and any default query parameters, header overrides, or logging
 * settings that apply specifically to that operation. Endpoint-level {@code logging} settings are merged on top of any
 * client-level {@code logging} settings, allowing fine-grained overrides per operation.
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * endpoints:
 *   create-payment:
 *     method: POST
 *     path: /api/v1/payments
 *     content-type: application/json
 *     accept: application/json
 *   list-payments:
 *     method: GET
 *     path: /api/v1/payments
 *     accept: application/json
 *     default-query-params:
 *       page: "0"
 *       size: "20"
 *     logging:
 *       request-body: false   # override: suppress body for this endpoint only
 * }</pre>
 *
 * @param method HTTP method for this endpoint (e.g. {@code GET}, {@code POST}, {@code PUT}, {@code PATCH},
 *     {@code DELETE}).
 * @param path URL path relative to the client's {@code base-url}. May contain URI template variables such as
 *     {@code /api/v1/payments/{id}}.
 * @param defaultQueryParams Default query parameters appended to every request targeting this endpoint. These are
 *     merged on top of any client-level {@code default-query-params}.
 * @param contentType Content-Type header override for this endpoint. When set, this takes precedence over the
 *     client-level {@code default-content-type}.
 * @param accept Accept header override for this endpoint. When set, this takes precedence over the client-level
 *     {@code default-accept}.
 * @param logging Logging overrides for this endpoint. Non-{@code null} fields override the corresponding client-level
 *     {@code logging} settings.
 */
public record EndpointProperties(
        String method,
        String path,
        Map<String, String> defaultQueryParams,
        String contentType,
        String accept,
        @NestedConfigurationProperty LoggingProperties logging) {}
