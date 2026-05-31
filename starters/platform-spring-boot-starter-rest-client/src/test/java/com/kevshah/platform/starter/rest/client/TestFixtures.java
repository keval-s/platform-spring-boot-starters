package com.kevshah.platform.starter.rest.client;

/// Shared test fixture records used across `PlatformRestClientTest` to exercise
/// Jackson serialization (request bodies) and deserialization (response bodies)
/// with typed Java objects instead of raw JSON strings.
final class TestFixtures {

    private TestFixtures() {}

    // -------------------------------------------------------------------------
    // Request payloads
    // -------------------------------------------------------------------------

    /// Request body carrying a `name` field — used for item creation endpoints.
    record CreateItemRequest(String name) {}

    /// Request body carrying a `status` field — used for update and partial-update endpoints.
    record UpdateItemRequest(String status) {}

    /// Request body carrying an `enabled` flag — used for toggle/patch endpoints.
    ///
    /// Exercises Jackson's serialization of `boolean` primitives.
    record ToggleRequest(boolean enabled) {}

    /// Request body carrying a `key` field — used for configuration replacement endpoints.
    record ConfigUpdateRequest(String key) {}

    // -------------------------------------------------------------------------
    // Response payloads
    // -------------------------------------------------------------------------

    /// Response body carrying an `id` field — returned by item-level endpoints.
    record ItemResponse(String id) {}

    /// Response body carrying a `status` field — returned by status-oriented endpoints.
    record StatusResponse(String status) {}
}

