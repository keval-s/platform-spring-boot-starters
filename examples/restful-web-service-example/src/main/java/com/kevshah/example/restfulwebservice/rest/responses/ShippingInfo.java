package com.kevshah.example.restfulwebservice.rest.responses;

import lombok.Builder;

@Builder
public record ShippingInfo(
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Address address
) {
}
