package com.kevshah.example.restclientexample.users;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes the Users API, delegating to {@link UserService} which uses the {@code users-client}
 * REST client configured via the platform rest-client starter.
 *
 * <p><b>Note:</b> This controller is not strictly necessary to demonstrate the REST client, but it provides a
 * convenient way to verify that the client is working as expected by exposing the same operations via HTTP endpoints
 * that can be easily tested with tools like {@code curl} or Postman.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

    private final UserService userService;

    /**
     * Constructs the controller with the required user service.
     *
     * @param userService service responsible for fetching users from the remote API
     */
    public UsersController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns all users fetched from the remote API.
     *
     * @return {@code 200 OK} with a JSON array of users
     */
    @GetMapping
    public ResponseEntity<List<User>> listUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }

    /**
     * Returns a single user by their identifier.
     *
     * @param id the user identifier
     * @return {@code 200 OK} with the matching user as JSON
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable int id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    /**
     * Creates a new user with the given details.
     *
     * @param user the user details for the new user
     * @return {@code 201 Created} with the created user as JSON
     */
    @PostMapping("")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(201).body(createdUser);
    }

    /**
     * Delete a user by their identifier.
     *
     * @param id the user identifier
     * @return {@code 204 No Content} if the user was successfully deleted
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable int id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
