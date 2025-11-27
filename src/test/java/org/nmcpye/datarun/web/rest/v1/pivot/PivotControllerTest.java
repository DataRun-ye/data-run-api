package org.nmcpye.datarun.web.rest.v1.pivot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Hamza Assada 23/08/2025 (7amza.it@gmail.com)
 */
@SpringBootTest
@AutoConfigureMockMvc
class PivotControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetPivotMetadata() throws Exception {
        mockMvc.perform(get("/api/pivot/metadata"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dimensions", hasSize(7))) // Matches your YAML config
            .andExpect(jsonPath("$.measures", hasSize(2)))
            .andExpect(jsonPath("$.dimensions[0].apiName").value("team_id"))
            .andExpect(jsonPath("$.measures[0].displayName").value("Numeric Value"));
    }
}
