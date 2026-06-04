package com.kevshah.example.restfulwebservice.rest.responses;

import java.util.List;
import lombok.Builder;

@Builder
public record OrderResponse(String orderId, List<LineItem> lineItems, BillingInfo billing, ShippingInfo shipping) {}
