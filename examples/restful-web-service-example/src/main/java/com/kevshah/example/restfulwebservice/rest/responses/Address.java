package com.kevshah.example.restfulwebservice.rest.responses;

import lombok.Builder;

@Builder
public record Address(
        String addressLine,
        String city,
        String regionCode,
        String postalCode,
        String countryCode
) {
}
