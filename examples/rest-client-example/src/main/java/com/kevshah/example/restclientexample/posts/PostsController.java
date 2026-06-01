package com.kevshah.example.restclientexample.posts;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/// REST controller that exposes the Posts API, delegating to [PostService] which
/// uses the `posts-client` REST client configured via the platform rest-client starter.
@RestController
@RequestMapping("/api/v1/posts")
public class PostsController {

    private final PostService postService;

    /// Constructs the controller with the required post service.
    ///
    /// @param postService service responsible for fetching posts from the remote API
    public PostsController(PostService postService) {
        this.postService = postService;
    }

    /// Returns a list of posts fetched from the remote API.
    ///
    /// The number of results is bounded by the `_limit` default query parameter configured
    /// on the `list-posts` endpoint.
    ///
    /// @return `200 OK` with a JSON array of posts
    @GetMapping
    public ResponseEntity<List<Post>> listPosts() {
        return ResponseEntity.ok(postService.listPosts());
    }

    /// Returns a single post by its identifier.
    ///
    /// @param id the post identifier
    /// @return `200 OK` with the matching post as JSON
    @GetMapping("/{id}")
    public ResponseEntity<Post> getPost(@PathVariable int id) {
        return ResponseEntity.ok(postService.getPost(id));
    }
}

