package com.trustplatform.volunteer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyVolunteerRequest {

    @NotNull
    private Long eventId;

    private String message;
}