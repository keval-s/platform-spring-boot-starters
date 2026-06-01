package com.kevshah.example.restclientexample.posts;

import com.kevshah.platform.starter.rest.client.PlatformRestClient;
import com.kevshah.platform.starter.rest.client.PlatformRestClientRegistry;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/// Service that fetches posts from the JSONPlaceholder API via the `posts-client`
/// configured under `platform.rest.client.clients.posts-client`.
///
/// Demonstrates named client injection, list retrieval with static default query
/// parameters (`_limit`), and single-resource lookup via URI template variable (`{id}`).
@Service
public class PostService {

    private final PlatformRestClient postsClient;

    /// Constructs the service and resolves the `posts-client` from the registry.
    ///
    /// @param registry the auto-configured registry holding all named REST clients
    public PostService(PlatformRestClientRegistry registry) {
        this.postsClient = registry.getPlatformRestClient("posts-client");
    }

    /// Returns a list of posts from the remote API.
    ///
    /// The `list-posts` endpoint is configured with a `_limit=10` default query parameter,
    /// so at most ten posts are returned per call unless the server ignores the parameter.
    ///
    /// @return list of posts
    public List<Post> listPosts() {
        return postsClient.get("list-posts", new ParameterizedTypeReference<>() {
        });
    }

    /// Returns a single post by its numeric identifier.
    ///
    /// The `{id}` URI template variable in the `get-post` endpoint path is expanded with
    /// the supplied `id` value.
    ///
    /// @param id the post identifier
    /// @return the matching post
    public Post getPost(int id) {
        return postsClient.get("get-post", Map.of("id", id), Post.class);
    }
}

