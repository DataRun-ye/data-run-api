package org.nmcpye.datarun.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@IntegrationTest
class UserAdminRouteSecurityIT {

    private static final String VERSIONED_ADMIN_USERS_URL = "/api/v1/admin/users/all";

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
}
