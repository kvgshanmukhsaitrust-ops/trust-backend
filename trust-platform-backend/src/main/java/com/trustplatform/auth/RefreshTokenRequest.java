package com.trustplatform.auth;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}