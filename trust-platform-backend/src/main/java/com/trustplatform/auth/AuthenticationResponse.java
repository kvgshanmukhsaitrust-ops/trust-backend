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
    
    private String token;          // The Access Token (JWT) — stored in localStorage by frontend

    @com.fasterxml.jackson.annotation.JsonIgnore  // Refresh token must NOT appear in the JSON body
    private String refreshToken;   // Internal only — used by AuthController to set the HttpOnly cookie
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