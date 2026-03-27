package com.trustplatform.member.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrustMemberResponse {
    private Long id;
    private String name;
    private String role;
    private String tagline;
    private String bio;
    private String imageUrl;
    private String twitterUrl;
    private String linkedinUrl;
}