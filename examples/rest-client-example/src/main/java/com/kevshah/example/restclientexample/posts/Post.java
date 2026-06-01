package com.kevshah.example.restclientexample.posts;

/// Represents a post returned by the JSONPlaceholder Posts API.
///
/// @param userId identifier of the user who authored the post
/// @param id     unique post identifier
/// @param title  title of the post
/// @param body   full text body of the post
public record Post(Integer userId, Integer id, String title, String body) {
}

