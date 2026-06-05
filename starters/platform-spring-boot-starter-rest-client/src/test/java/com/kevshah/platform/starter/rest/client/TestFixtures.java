package com.kevshah.platform.starter.rest.client;

/**
 * Shared test fixture records used across {@code PlatformRestClientTest} to exercise Jackson serialization (request
 * bodies) and deserialization (response bodies) with typed Java objects instead of raw JSON strings.
 */
final class TestFixtures {

    private TestFixtures() {}

    // -------------------------------------------------------------------------
    // Request payloads
    // -------------------------------------------------------------------------

    /** Request body carrying a {@code name} field &mdash; used for item creation endpoints. */
    record CreateItemRequest(String name) {}

    /** Request body carrying a {@code status} field &mdash; used for update and partial-update endpoints. */
    record UpdateItemRequest(String status) {}

    /**
     * Request body carrying an {@code enabled} flag &mdash; used for toggle/patch endpoints.
     *
     * <p>Exercises Jackson's serialization of {@code boolean} primitives.
     */
    record ToggleRequest(boolean enabled) {}

    /** Request body carrying a {@code key} field &mdash; used for configuration replacement endpoints. */
    record ConfigUpdateRequest(String key) {}

    // -------------------------------------------------------------------------
    // Response payloads
    // -------------------------------------------------------------------------

    /** Response body carrying an {@code id} field &mdash; returned by item-level endpoints. */
    record ItemResponse(String id) {}

    /** Response body carrying a {@code status} field &mdash; returned by status-oriented endpoints. */
    record StatusResponse(String status) {}
}
