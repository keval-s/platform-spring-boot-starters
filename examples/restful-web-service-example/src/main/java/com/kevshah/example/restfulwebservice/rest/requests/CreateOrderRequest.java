package com.kevshah.example.restfulwebservice.rest.requests;

import java.util.List;

public record CreateOrderRequest(
    List<LineItem> lineItems,
    BillingInfo billing,
    ShippingInfo shipping
) {}
