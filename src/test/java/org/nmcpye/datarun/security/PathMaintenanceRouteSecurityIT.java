package org.nmcpye.datarun.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The maintenance endpoints rewrite a whole table: stored paths, or every
 * team's form permissions. Their resources admit field users for ordinary
 * reads; the maintenance must not.
 */
@AutoConfigureMockMvc
@IntegrationTest
class PathMaintenanceRouteSecurityIT {

    private static final String[] PATH_RECOMPUTE_URLS = {
        "/api/v1/orgUnits/updatePaths?forceUpdate=true",
        "/api/v1/assignments/updatePaths?forceUpdate=true",
        "/api/v1/teams/migrate",
        "/api/custom/teams/migrate",
    };

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void deniesPathRecomputeToFieldUsers() throws Exception {
        for (String url : PATH_RECOMPUTE_URLS) {
            mockMvc.perform(get(url)).andExpect(status().isForbidden());
        }
    }
}
