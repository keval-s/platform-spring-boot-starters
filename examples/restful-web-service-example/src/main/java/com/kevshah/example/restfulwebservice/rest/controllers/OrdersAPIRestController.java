package com.kevshah.example.restfulwebservice.rest.controllers;

import com.kevshah.example.restfulwebservice.rest.requests.CreateOrderRequest;
import com.kevshah.example.restfulwebservice.rest.responses.Address;
import com.kevshah.example.restfulwebservice.rest.responses.BillingInfo;
import com.kevshah.example.restfulwebservice.rest.responses.CreateOrderResponse;
import com.kevshah.example.restfulwebservice.rest.responses.LineItem;
import com.kevshah.example.restfulwebservice.rest.responses.OrderResponse;
import com.kevshah.example.restfulwebservice.rest.responses.ShippingInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class OrdersAPIRestController {


    @PostMapping("/orders")
    public ResponseEntity<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
        // Returning a mocked response
        String orderId = UUID.ofEpochMillis(Instant.now().toEpochMilli()).toString();
        return ResponseEntity.created(URI.create("/api/v1/orders/" + orderId))
                .body(new CreateOrderResponse(orderId));
    }


    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        // Returning a mocked response
        return ResponseEntity.ok(OrderResponse.builder()
                .orderId(orderId)
                .lineItems(List.of(LineItem.builder()
                        .lineId(UUID.ofEpochMillis(Instant.now().toEpochMilli()).toString())
                        .sku("SKU-123")
                        .quantity(2)
                        .build()
                ))
                .billing(BillingInfo.builder()
                        .firstName("First")
                        .lastName("Last")
                        .email("billing@example.com")
                        .phoneNumber("123-456-7890")
                        .paymentId("PAY-1234567890")
                        .address(Address.builder()
                                .addressLine("123 Test St.")
                                .city("Test City")
                                .regionCode("BC")
                                .postalCode("V6B 6B1")
                                .countryCode("CA")
                                .build())
                        .build())
                .shipping(ShippingInfo.builder()
                        .firstName("First")
                        .lastName("Last")
                        .email("shipping@example.com")
                        .phoneNumber("123-456-7890")
                        .address(Address.builder()
                                .addressLine("123 Test St.")
                                .city("Test City")
                                .regionCode("BC")
                                .postalCode("V6B 6B1")
                                .countryCode("CA")
                                .build()
                        )
                        .build())
                .build());
    }


}
