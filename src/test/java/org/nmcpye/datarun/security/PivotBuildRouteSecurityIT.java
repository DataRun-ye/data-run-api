package org.nmcpye.datarun.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The pivot build drops, creates and renames tables in the pivot schema. The
 * general /api rule admits every logged-in account; the build must admit
 * admins only.
 */
@AutoConfigureMockMvc
@IntegrationTest
class PivotBuildRouteSecurityIT {

    private static final String PIVOT_BUILD_URL = "/api/v1/pivot/build_by_template";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void deniesPivotBuildToFieldUsers() throws Exception {
        mockMvc
            .perform(post(PIVOT_BUILD_URL).param("template_uid", "tmpl0000001"))
            .andExpect(status().isForbidden());
    }

    // The controller itself refuses an empty template list, so the request
    // passed security and nothing is built.
    @Test
    @WithMockUser(authorities = AuthoritiesConstants.ADMIN)
    void allowsPivotBuildRequestToReachTheControllerForAdmins() throws Exception {
        mockMvc
            .perform(post(PIVOT_BUILD_URL).param("template_uid", ""))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("{}"));
    }
}
