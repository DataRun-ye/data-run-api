package org.nmcpye.datarun.security;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The team PATCH route replaces a team's members and settings. TeamResource
 * admits field users for ordinary reads; the rewrite must not, or a field user
 * could join any team (and, through it, the teams it manages).
 */
@AutoConfigureMockMvc
@IntegrationTest
class TeamWriteRouteSecurityIT {
    private static final String[] TEAM_PATCH_URLS = {
        "/api/v1/teams/teams/team0000001",
        "/api/custom/teams/teams/team0000001",
    };

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void deniesTeamRewritesToFieldUsers() throws Exception {
        for (String url : TEAM_PATCH_URLS) {
            mockMvc.perform(patch(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"uid\":\"team0000001\",\"users\":[{\"login\":\"worker\"}]}"))
                .andExpect(status().isForbidden());
        }
    }
}
