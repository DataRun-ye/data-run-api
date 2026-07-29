package org.nmcpye.datarun.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@IntegrationTest
class UserAdminRouteSecurityIT {

    private static final String VERSIONED_ADMIN_USERS_URL = "/api/v1/admin/users/all";
    private static final String[] USER_CREATION_URLS = {
        "/api/custom/register",
        "/api/v1/register",
    };

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void deniesVersionedAdminRouteToUser() throws Exception {
        mockMvc.perform(get(VERSIONED_ADMIN_USERS_URL)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.ADMIN)
    void allowsVersionedAdminRouteToAdmin() throws Exception {
        mockMvc.perform(get(VERSIONED_ADMIN_USERS_URL)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void deniesUnmappedRoutesInsteadOfFallingThroughSecurity() throws Exception {
        mockMvc
            .perform(get("/unmapped-transition-route"))
            .andExpect(status().isForbidden());
    }

    @Test
    void deniesUserCreationToAnonymousCallers() throws Exception {
        for (String url : USER_CREATION_URLS) {
            mockMvc
                .perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void deniesUserCreationToFieldUsers() throws Exception {
        for (String url : USER_CREATION_URLS) {
            mockMvc
                .perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.ADMIN)
    void allowsUserCreationRequestToReachAdminValidation() throws Exception {
        for (String url : USER_CREATION_URLS) {
            mockMvc
                .perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        }
    }
}
