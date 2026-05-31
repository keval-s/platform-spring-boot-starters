package com.kevshah.example.restfulwebservice.rest.responses;

import lombok.Builder;

@Builder
public record BillingInfo(
        String paymentId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Address address
) {
}
