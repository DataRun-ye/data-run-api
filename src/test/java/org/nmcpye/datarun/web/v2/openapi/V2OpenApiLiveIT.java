package org.nmcpye.datarun.web.v2.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.v2.config.V2OpenApiConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The running application serves the published v2 contract: the document the unit test generates from the v2
 * controllers alone is the one the whole application serves, test fixtures hidden.
 */
@IntegrationTest
@AutoConfigureMockMvc
class V2OpenApiLiveIT {

    @Autowired
    private MockMvc mvc;

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.ADMIN)
    void theApplicationServesThePublishedContract() throws Exception {
        MockHttpServletResponse response = mvc.perform(get(V2OpenApiConfiguration.DOCS_PATH)).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        JsonNode served = OpenApiDocuments.read(response.getContentAsString());

        assertThat(served).isEqualTo(OpenApiDocuments.readSnapshot());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void onlyAdminsReadTheDocument() throws Exception {
        assertThat(mvc.perform(get(V2OpenApiConfiguration.DOCS_PATH)).andReturn().getResponse().getStatus())
            .isEqualTo(403);
    }
}
