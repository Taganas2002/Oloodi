package com.oloodi.livestaffing.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oloodi.livestaffing.assignment.domain.AttendanceStatus;
import com.oloodi.livestaffing.assignment.domain.ParticipationStatus;
import com.oloodi.livestaffing.assignment.domain.ShiftAssignment;
import com.oloodi.livestaffing.assignment.repository.ShiftAssignmentRepository;
import com.oloodi.livestaffing.support.AbstractPostgresIntegrationTest;
import com.oloodi.livestaffing.support.DemoIds;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

@AutoConfigureMockMvc
@DisplayName("Phase 4: natural HTTP idempotency (no Idempotency-Key)")
class IdempotencyHttpIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShiftAssignmentRepository assignmentRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void reset() {
        resetAssignment(DemoIds.ASSIGNMENT_SARAH);
        resetAssignment(DemoIds.ASSIGNMENT_DANIEL);
        resetAssignment(DemoIds.ASSIGNMENT_ALEX);
    }

    private void resetAssignment(UUID id) {
        transactionTemplate.executeWithoutResult(status -> {
            ShiftAssignment a = assignmentRepository
                    .findByShiftIdAndIdWithWorker(DemoIds.SHIFT_ID, id)
                    .orElseThrow();
            a.applyState(
                    ParticipationStatus.UNINVITED,
                    AttendanceStatus.NOT_STARTED,
                    null,
                    null,
                    Instant.parse("2026-11-01T12:00:00Z"));
            assignmentRepository.saveAndFlush(a);
        });
    }

    private String base(UUID assignmentId) {
        return "/api/shifts/" + DemoIds.SHIFT_ID + "/assignments/" + assignmentId;
    }

    @Test
    void duplicateInviteDoesNotCreateRowsAndKeepsState() throws Exception {
        mockMvc.perform(post(base(DemoIds.ASSIGNMENT_SARAH) + "/invite")).andExpect(status().isOk());
        mockMvc.perform(post(base(DemoIds.ASSIGNMENT_SARAH) + "/invite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participationStatus").value("INVITED"));

        mockMvc.perform(get("/api/shifts/" + DemoIds.SHIFT_ID + "/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void duplicateCheckInAndCheckOutKeepTimestamps() throws Exception {
        UUID id = DemoIds.ASSIGNMENT_DANIEL;
        mockMvc.perform(post(base(id) + "/invite")).andExpect(status().isOk());
        mockMvc.perform(post(base(id) + "/accept")).andExpect(status().isOk());

        MvcResult in1 = mockMvc.perform(post(base(id) + "/check-in"))
                .andExpect(status().isOk())
                .andReturn();
        String checkedInAt = objectMapper
                .readTree(in1.getResponse().getContentAsString())
                .get("checkedInAt")
                .asText();

        mockMvc.perform(post(base(id) + "/check-in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedInAt").value(checkedInAt))
                .andExpect(jsonPath("$.attendanceStatus").value("CHECKED_IN"));

        MvcResult out1 = mockMvc.perform(post(base(id) + "/check-out"))
                .andExpect(status().isOk())
                .andReturn();
        String checkedOutAt = objectMapper
                .readTree(out1.getResponse().getContentAsString())
                .get("checkedOutAt")
                .asText();

        mockMvc.perform(post(base(id) + "/check-out"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedOutAt").value(checkedOutAt))
                .andExpect(jsonPath("$.checkedInAt").value(checkedInAt));

        MvcResult dash = mockMvc.perform(get("/api/shifts/" + DemoIds.SHIFT_ID + "/dashboard"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode counts = objectMapper.readTree(dash.getResponse().getContentAsString()).get("counts");
        assertThat(counts.get("checkedOut").asInt()).isEqualTo(1);
        assertThat(assignmentRepository.findAllByShiftIdWithWorker(DemoIds.SHIFT_ID)).hasSize(3);
    }
}
