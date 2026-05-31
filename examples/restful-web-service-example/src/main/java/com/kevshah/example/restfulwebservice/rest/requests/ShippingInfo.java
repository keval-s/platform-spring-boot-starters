package com.kevshah.example.restfulwebservice.rest.requests;

public record ShippingInfo(
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Address address
) {
}
