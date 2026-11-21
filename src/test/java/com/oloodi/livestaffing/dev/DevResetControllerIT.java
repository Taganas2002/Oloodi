package com.oloodi.livestaffing.dev;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oloodi.livestaffing.support.AbstractPostgresIntegrationTest;
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

    @Test
    void resetEndpointRestoresSeedAssignments() throws Exception {
        mockMvc.perform(post("/api/dev/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.updatedAssignments").value(3));
    }
}
