package com.trustplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustplatform.audit.AuditLog;
import com.trustplatform.audit.AuditLogRepository;
import com.trustplatform.event.dto.CreateEventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuditSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        auditLogRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "regular@user.org", authorities = {"READ_CONTENT"})
    public void whenRegularUserAttemptsAccessToAuditLogs_thenReturns403AndLogsSecurityViolation() throws Exception {
        // Attempt accessing protected Audit logs endpoint
        mockMvc.perform(get("/api/admin/activities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Wait a brief moment since audit logs are written asynchronously (@Async)
        Thread.sleep(150);

        // Verify that a SECURITY_VIOLATION FAILED audit log was generated
        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).isNotEmpty();
        
        AuditLog violationLog = logs.stream()
                .filter(l -> "SECURITY_VIOLATION".equals(l.getAction()))
                .findFirst()
                .orElse(null);

        assertThat(violationLog).isNotNull();
        assertThat(violationLog.getStatus()).isEqualTo("FAILED");
        assertThat(violationLog.getTargetResource()).isEqualTo("Security");
        assertThat(violationLog.getPerformedBy()).isEqualTo("regular@user.org");
        assertThat(violationLog.getDetails()).contains("/api/admin/activities");
    }

    @Test
    @WithMockUser(username = "admin@user.org", authorities = {"MANAGE_EVENTS"})
    public void whenAdminSubmitsInvalidEventData_thenReturns400AndLogsValidationFailure() throws Exception {
        // Payload lacks title and other required parameters, triggering validation exception
        CreateEventRequest request = CreateEventRequest.builder().build();

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Wait for async persistence
        Thread.sleep(150);

        // Verify that a VALIDATION_FAILURE audit log was saved
        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).isNotEmpty();

        AuditLog validationLog = logs.stream()
                .filter(l -> "VALIDATION_FAILURE".equals(l.getAction()))
                .findFirst()
                .orElse(null);

        assertThat(validationLog).isNotNull();
        assertThat(validationLog.getStatus()).isEqualTo("FAILED");
        assertThat(validationLog.getTargetResource()).isEqualTo("Validation");
        assertThat(validationLog.getPerformedBy()).isEqualTo("admin@user.org");
        assertThat(validationLog.getDetails()).contains("/api/events");
    }
}
