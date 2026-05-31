package com.kevshah.example.restfulwebservice.rest.responses;

import lombok.Builder;

import java.util.List;

@Builder
public record OrderResponse(
        String orderId,
        List<LineItem> lineItems,
        BillingInfo billing,
        ShippingInfo shipping
) {
}
