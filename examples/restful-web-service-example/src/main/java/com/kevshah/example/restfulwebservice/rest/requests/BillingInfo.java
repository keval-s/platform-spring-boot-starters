package com.kevshah.example.restfulwebservice.rest.requests;

public record BillingInfo(
        String paymentId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Address address
) {
}
