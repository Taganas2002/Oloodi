package com.oloodi.livestaffing.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oloodi.livestaffing.assignment.domain.AttendanceStatus;
import com.oloodi.livestaffing.assignment.domain.ParticipationStatus;
import com.oloodi.livestaffing.assignment.repository.ShiftAssignmentRepository;
import com.oloodi.livestaffing.shift.domain.Shift;
import com.oloodi.livestaffing.shift.repository.ShiftRepository;
import com.oloodi.livestaffing.support.AbstractPostgresIntegrationTest;
import com.oloodi.livestaffing.support.DemoIds;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@ActiveProfiles("test")
class DevResetControllerIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ShiftAssignmentRepository assignmentRepository;

    @Autowired
    private Clock clock;

    @Test
    void resetEndpointRestoresSeedAssignments() throws Exception {
        mockMvc.perform(post("/api/dev/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.updatedAssignments").value(3));

        DemoShiftSchedule.Window expected = DemoShiftSchedule.windowFor(clock);
        Shift shift = shiftRepository.findById(DemoIds.SHIFT_ID).orElseThrow();
        assertThat(shift.getStartsAt()).isEqualTo(expected.startsAt());
        assertThat(shift.getEndsAt()).isEqualTo(expected.endsAt());

        assertThat(assignmentRepository.findAllByShiftIdWithWorker(DemoIds.SHIFT_ID))
                .hasSize(3)
                .allMatch(a -> a.getParticipationStatus() == ParticipationStatus.INVITED)
                .allMatch(a -> a.getAttendanceStatus() == AttendanceStatus.NOT_STARTED)
                .allMatch(a -> a.getCheckedInAt() == null)
                .allMatch(a -> a.getCheckedOutAt() == null);
    }
}
