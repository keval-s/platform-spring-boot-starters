package com.kevshah.example.restfulwebservice.rest.responses;

import lombok.Builder;

@Builder
public record LineItem(
        String lineId,
        String sku,
        Integer quantity
) {
}
