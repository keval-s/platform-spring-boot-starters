package com.kevshah.example.restclientexample.users;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/// REST controller that exposes the Users API, delegating to [UserService] which
/// uses the `users-client` REST client configured via the platform rest-client starter.
@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

    private final UserService userService;

    /// Constructs the controller with the required user service.
    ///
    /// @param userService service responsible for fetching users from the remote API
    public UsersController(UserService userService) {
        this.userService = userService;
    }

    /// Returns all users fetched from the remote API.
    ///
    /// @return `200 OK` with a JSON array of users
    @GetMapping
    public ResponseEntity<List<User>> listUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }

    /// Returns a single user by their identifier.
    ///
    /// @param id the user identifier
    /// @return `200 OK` with the matching user as JSON
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable int id) {
        return ResponseEntity.ok(userService.getUser(id));
    }
}

