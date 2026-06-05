package com.kevshah.example.restclientexample.posts;

import com.kevshah.platform.starter.rest.client.PlatformRestClient;
import com.kevshah.platform.starter.rest.client.PlatformRestClientRegistry;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

/**
 * Service that fetches posts from the JSONPlaceholder API via the {@code posts-client} configured under
 * {@code platform.rest.client.clients.posts-client}.
 *
 * <p>Demonstrates named client injection, list retrieval with static default query parameters ({@code _limit}), and
 * single-resource lookup via URI template variable ({@code {id}}).
 */
@Service
public class PostService {

    private final PlatformRestClient postsClient;

    /**
     * Constructs the service and resolves the {@code posts-client} from the registry.
     *
     * @param registry the auto-configured registry holding all named REST clients
     */
    public PostService(PlatformRestClientRegistry registry) {
        this.postsClient = registry.getPlatformRestClient("posts-client");
    }

    /**
     * Returns a list of posts from the remote API.
     *
     * <p>The {@code list-posts} endpoint is configured with a {@code _limit=10} default query parameter, so at most ten
     * posts are returned per call unless the server ignores the parameter.
     *
     * @return list of posts
     */
    public List<Post> listPosts() {
        return postsClient.get("list-posts", new ParameterizedTypeReference<>() {});
    }

    /**
     * Returns a single post by its numeric identifier.
     *
     * <p>The {@code {id}} URI template variable in the {@code get-post} endpoint path is expanded with the supplied
     * {@code id} value.
     *
     * @param id the post identifier
     * @return the matching post
     */
    public Post getPost(int id) {
        return postsClient.get("get-post", Map.of("id", id), Post.class);
    }
}
