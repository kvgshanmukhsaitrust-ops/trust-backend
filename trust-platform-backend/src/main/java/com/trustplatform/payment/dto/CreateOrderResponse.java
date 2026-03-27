package com.trustplatform.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateOrderResponse {

    private String orderId;
    private String key;
    private Long amount;
    private String currency;
}