package com.oloodi.livestaffing.assignment.web;

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

@AutoConfigureMockMvc
class AssignmentApiIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShiftAssignmentRepository assignmentRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void resetSeedAssignments() {
        resetAssignment(DemoIds.ASSIGNMENT_SARAH);
        resetAssignment(DemoIds.ASSIGNMENT_DANIEL);
        resetAssignment(DemoIds.ASSIGNMENT_ALEX);
    }

    private void resetAssignment(UUID assignmentId) {
        transactionTemplate.executeWithoutResult(status -> {
            ShiftAssignment a = assignmentRepository
                    .findByShiftIdAndIdWithWorker(DemoIds.SHIFT_ID, assignmentId)
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

    @Nested
    @DisplayName("A. Legal transitions via REST")
    class LegalViaRest {

        @Test
        void fullHappyPathInviteAcceptCheckInCheckOut() throws Exception {
            UUID id = DemoIds.ASSIGNMENT_SARAH;

            mockMvc.perform(post(base(id) + "/invite"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.participationStatus").value("INVITED"));

            mockMvc.perform(post(base(id) + "/accept"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.participationStatus").value("ACCEPTED"))
                    .andExpect(jsonPath("$.attendanceStatus").value("NOT_STARTED"));

            MvcResult checkIn = mockMvc.perform(post(base(id) + "/check-in"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.participationStatus").value("ACCEPTED"))
                    .andExpect(jsonPath("$.attendanceStatus").value("CHECKED_IN"))
                    .andExpect(jsonPath("$.checkedInAt").isNotEmpty())
                    .andReturn();

            String checkedInAt = objectMapper
                    .readTree(checkIn.getResponse().getContentAsString())
                    .get("checkedInAt")
                    .asText();

            mockMvc.perform(post(base(id) + "/check-out"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attendanceStatus").value("CHECKED_OUT"))
                    .andExpect(jsonPath("$.checkedInAt").value(checkedInAt))
                    .andExpect(jsonPath("$.checkedOutAt").isNotEmpty());
        }

        @Test
        void inviteDecline() throws Exception {
            UUID id = DemoIds.ASSIGNMENT_DANIEL;
            mockMvc.perform(post(base(id) + "/invite")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/decline"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.participationStatus").value("DECLINED"));
        }

        @Test
        void acceptCancelAcceptAgain() throws Exception {
            UUID id = DemoIds.ASSIGNMENT_ALEX;
            mockMvc.perform(post(base(id) + "/invite")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/accept")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.participationStatus").value("CANCELLED"));
            mockMvc.perform(post(base(id) + "/accept"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.participationStatus").value("ACCEPTED"));
        }
    }

    @Nested
    @DisplayName("B. Illegal transitions return 409")
    class IllegalViaRest {

        @Test
        void uninvitedAcceptIsConflict() throws Exception {
            mockMvc.perform(post(base(DemoIds.ASSIGNMENT_SARAH) + "/accept"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.detail").exists());
        }

        @Test
        void uninvitedCheckInIsConflict() throws Exception {
            mockMvc.perform(post(base(DemoIds.ASSIGNMENT_SARAH) + "/check-in"))
                    .andExpect(status().isConflict());
        }

        @Test
        void declinedCheckInIsConflict() throws Exception {
            UUID id = DemoIds.ASSIGNMENT_SARAH;
            mockMvc.perform(post(base(id) + "/invite")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/decline")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/check-in")).andExpect(status().isConflict());
        }

        @Test
        void notStartedCheckOutIsConflict() throws Exception {
            UUID id = DemoIds.ASSIGNMENT_SARAH;
            mockMvc.perform(post(base(id) + "/invite")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/accept")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/check-out")).andExpect(status().isConflict());
        }

        @Test
        void checkedOutCheckInIsConflict() throws Exception {
            UUID id = DemoIds.ASSIGNMENT_SARAH;
            mockMvc.perform(post(base(id) + "/invite")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/accept")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/check-in")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/check-out")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/check-in")).andExpect(status().isConflict());
        }

        @Test
        void checkedInCancelIsConflict() throws Exception {
            UUID id = DemoIds.ASSIGNMENT_SARAH;
            mockMvc.perform(post(base(id) + "/invite")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/accept")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/check-in")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/cancel")).andExpect(status().isConflict());
        }

        @Test
        void checkedOutCancelIsConflict() throws Exception {
            UUID id = DemoIds.ASSIGNMENT_SARAH;
            mockMvc.perform(post(base(id) + "/invite")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/accept")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/check-in")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/check-out")).andExpect(status().isOk());
            mockMvc.perform(post(base(id) + "/cancel")).andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("C. Idempotency via REST")
    class IdempotencyViaRest {

        @Test
        void inviteTwiceNoDuplicateAndSameState() throws Exception {
            UUID id = DemoIds.ASSIGNMENT_SARAH;
            MvcResult first = mockMvc.perform(post(base(id) + "/invite"))
                    .andExpect(status().isOk())
                    .andReturn();
            MvcResult second = mockMvc.perform(post(base(id) + "/invite"))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode f = objectMapper.readTree(first.getResponse().getContentAsString());
            JsonNode s = objectMapper.readTree(second.getResponse().getContentAsString());
            assertThat(s.get("participationStatus").asText()).isEqualTo("INVITED");
            assertThat(s.get("id").asText()).isEqualTo(f.get("id").asText());

            mockMvc.perform(get("/api/shifts/" + DemoIds.SHIFT_ID + "/assignments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3));
        }

        @Test
        void checkInAndCheckOutTwiceKeepTimestamps() throws Exception {
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
                    .andExpect(jsonPath("$.checkedInAt").value(checkedInAt));

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
        }
    }

    @Nested
    @DisplayName("D. Counts vs assignment list")
    class CountsVsList {

        @Test
        void dashboardCountsMatchAssignments() throws Exception {
            // Sarah: ACCEPTED + CHECKED_IN
            mockMvc.perform(post(base(DemoIds.ASSIGNMENT_SARAH) + "/invite")).andExpect(status().isOk());
            mockMvc.perform(post(base(DemoIds.ASSIGNMENT_SARAH) + "/accept")).andExpect(status().isOk());
            mockMvc.perform(post(base(DemoIds.ASSIGNMENT_SARAH) + "/check-in")).andExpect(status().isOk());

            // Daniel: ACCEPTED + NOT_STARTED
            mockMvc.perform(post(base(DemoIds.ASSIGNMENT_DANIEL) + "/invite")).andExpect(status().isOk());
            mockMvc.perform(post(base(DemoIds.ASSIGNMENT_DANIEL) + "/accept")).andExpect(status().isOk());

            // Alex: INVITED
            mockMvc.perform(post(base(DemoIds.ASSIGNMENT_ALEX) + "/invite")).andExpect(status().isOk());

            MvcResult dashboard = mockMvc.perform(get("/api/shifts/" + DemoIds.SHIFT_ID + "/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.shift.title").value("Oloodi Event Staffing"))
                    .andExpect(jsonPath("$.assignments.length()").value(3))
                    .andExpect(jsonPath("$.counts.filled").value(2))
                    .andExpect(jsonPath("$.counts.remaining").value(1))
                    .andExpect(jsonPath("$.counts.checkedIn").value(1))
                    .andExpect(jsonPath("$.counts.checkedOut").value(0))
                    .andReturn();

            JsonNode root = objectMapper.readTree(dashboard.getResponse().getContentAsString());
            long accepted = 0;
            long checkedIn = 0;
            long checkedOut = 0;
            for (JsonNode a : root.get("assignments")) {
                if ("ACCEPTED".equals(a.get("participationStatus").asText())) {
                    accepted++;
                }
                String attendance = a.get("attendanceStatus").asText();
                if ("CHECKED_IN".equals(attendance)) {
                    checkedIn++;
                }
                if ("CHECKED_OUT".equals(attendance)) {
                    checkedOut++;
                }
            }
            assertThat(accepted).isEqualTo(root.get("counts").get("filled").asLong());
            assertThat(checkedIn).isEqualTo(root.get("counts").get("checkedIn").asLong());
            assertThat(checkedOut).isEqualTo(root.get("counts").get("checkedOut").asLong());
            int required = root.get("shift").get("requiredWorkers").asInt();
            assertThat(root.get("counts").get("remaining").asInt())
                    .isEqualTo(Math.max(required - (int) accepted, 0));
        }
    }

    @Test
    void unknownShiftReturns404() throws Exception {
        UUID missing = UUID.fromString("99999999-9999-9999-9999-999999999999");
        mockMvc.perform(get("/api/shifts/" + missing + "/dashboard"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void malformedUuidReturns400() throws Exception {
        mockMvc.perform(get("/api/shifts/not-a-uuid/dashboard"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getAssignmentReturnsDtoOnly() throws Exception {
        mockMvc.perform(get(base(DemoIds.ASSIGNMENT_SARAH)).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerName").value("Sarah Johnson"))
                .andExpect(jsonPath("$.participationStatus").value("UNINVITED"))
                .andExpect(jsonPath("$.shift").doesNotExist());
    }
}
