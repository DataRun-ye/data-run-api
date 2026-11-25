package org.nmcpye.datarun.config.activation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.v2.fixtures.V2ActivationFixture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** In the running application a declared switch nobody set is off, and its v2 operation answers 422. */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.USER)
class ActivationSwitchesIT {

    @Autowired
    private ActivationSwitches switches;

    @Autowired
    private MockMvc mvc;

    @Test
    void aSwitchNobodySetIsOffAndItsOperationIsRefused() throws Exception {
        assertThat(switches.isOn(V2ActivationFixture.FIXTURE_ACTION)).isFalse();

        MockHttpServletResponse response = mvc.perform(post("/api/v2/fixtures/switched:run")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(422);
        JsonNode problem = new ObjectMapper().readTree(response.getContentAsByteArray());
        assertThat(problem.get("code").asText()).isEqualTo("method-not-enabled");
        assertThat(problem.get("detail").asText()).contains("fixture-action");

        MockHttpServletResponse gated = mvc.perform(post("/api/v2/fixtures/switched:apply").param("op", "gated"))
            .andReturn().getResponse();
        assertThat(gated.getStatus()).isEqualTo(422);
        assertThat(new ObjectMapper().readTree(gated.getContentAsByteArray()).get("code").asText())
            .isEqualTo("operation-not-enabled");
        assertThat(mvc.perform(post("/api/v2/fixtures/switched:apply").param("op", "open")).andReturn()
            .getResponse().getStatus()).isEqualTo(200);
    }
}
