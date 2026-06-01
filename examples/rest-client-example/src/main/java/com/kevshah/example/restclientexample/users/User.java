package com.kevshah.example.restclientexample.users;

/// Represents a user returned by the JSONPlaceholder Users API.
///
/// Nested objects (`address`, `company`) returned by the API are intentionally
/// omitted; Jackson ignores unknown properties by default.
///
/// @param id       unique user identifier
/// @param name     full display name
/// @param username login handle
/// @param email    email address
/// @param phone    contact phone number
/// @param website  personal website URL
public record User(Integer id, String name, String username, String email,
                   String phone, String website) {
}

