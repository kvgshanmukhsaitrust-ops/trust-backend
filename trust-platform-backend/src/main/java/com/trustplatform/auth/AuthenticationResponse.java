package com.trustplatform.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    
    private String token;          // The Access Token (JWT)
    private String refreshToken;   // ADD THIS FIELD TO FIX THE BUILDER ERROR
    private UserDto user;          // Nested object for frontend AppContext

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserDto {
        private String name;
        private String email;
        private String role;
    }
}