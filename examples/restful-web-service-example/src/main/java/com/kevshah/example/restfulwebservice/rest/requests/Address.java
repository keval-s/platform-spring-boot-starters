package com.kevshah.example.restfulwebservice.rest.requests;

public record Address(
        String addressLine,
        String city,
        String regionCode,
        String postalCode,
        String countryCode
) {
}
