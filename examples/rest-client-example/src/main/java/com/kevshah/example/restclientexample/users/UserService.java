package com.kevshah.example.restclientexample.users;

import com.kevshah.platform.starter.rest.client.PlatformRestClient;
import com.kevshah.platform.starter.rest.client.PlatformRestClientRegistry;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

/// Service that fetches users from the JSONPlaceholder API via the `users-client`
/// configured under `platform.rest.client.clients.users-client`.
///
/// Demonstrates using a second named client alongside `posts-client`, showing that
/// multiple independent REST clients can coexist in the same application.
@Service
public class UserService {

    private final PlatformRestClient usersClient;

    /// Constructs the service and resolves the `users-client` from the registry.
    ///
    /// @param registry the auto-configured registry holding all named REST clients
    public UserService(PlatformRestClientRegistry registry) {
        this.usersClient = registry.getPlatformRestClient("users-client");
    }

    /// Returns all users from the remote API.
    ///
    /// @return list of users
    public List<User> listUsers() {
        return usersClient.get("list-users", new ParameterizedTypeReference<>() {});
    }

    /// Returns a single user by their numeric identifier.
    ///
    /// The `{id}` URI template variable in the `get-user` endpoint path is expanded with
    /// the supplied `id` value.
    ///
    /// @param id the user identifier
    /// @return the matching user
    public User getUser(int id) {
        return usersClient.get("get-user", Map.of("id", id), User.class);
    }

    /// Creates a new user with the given details.
    ///
    /// The user details are sent as JSON in the request body, and returned in the response body.
    ///
    /// @param user the user details for the new user
    /// @return the created user
    public User createUser(User user) {
        return usersClient.post("create-user", user, User.class);
    }

    /// Delete a user by their identifier
    ///
    /// @param id the user identifier
    public void deleteUser(int id) {
        usersClient.delete("delete-user", Map.of("id", id));
    }
}
