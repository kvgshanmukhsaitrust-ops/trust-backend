package com.trustplatform.payment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebhookPayload {

    private String event;
    private Payload payload;

    @Getter
    @Setter
    public static class Payload {
        private Payment payment;
    }

    @Getter
    @Setter
    public static class Payment {
        private Entity entity;
    }

    @Getter
    @Setter
    public static class Entity {

        private String id;

        private String order_id;

        private String status;

        private Integer amount;
    }
}