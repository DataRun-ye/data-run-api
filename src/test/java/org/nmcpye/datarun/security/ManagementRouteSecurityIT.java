package org.nmcpye.datarun.security;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.config.EmbeddedSQL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Who may read the management routes. Only health and info are public. Prometheus metrics need an admin since
 * 6.5.5: they had been public, and they name every route pattern, repository timings, exception types and error
 * rates. The endpoints are exposed as application.yml exposes them in production, and Prometheus export is on as
 * production's compose file turns it on ({@code MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED=true}, which outranks
 * application-prod.yml's {@code false}).
 */
@AutoConfigureMockMvc
@IntegrationTest
@TestPropertySource(properties = {
    "management.endpoints.web.base-path=/management",
    "management.endpoints.web.exposure.include=health,info,prometheus",
    "management.prometheus.metrics.export.enabled=true"
})
class ManagementRouteSecurityIT {

    @Autowired
    private MockMvc mvc;

    @Test
    void anAnonymousScrapeIsRefused() throws Exception {
        mvc.perform(get("/management/prometheus")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void aFieldUsersScrapeIsForbidden() throws Exception {
        mvc.perform(get("/management/prometheus")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.ADMIN)
    void anAdminScrapesTheMetrics() throws Exception {
        mvc.perform(get("/management/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("jvm_memory_used_bytes")));
    }

    @Test
    void healthAndInfoStayPublic() throws Exception {
        mvc.perform(get("/management/health")).andExpect(status().isOk());
        mvc.perform(get("/management/info")).andExpect(status().isOk());
    }

    /**
     * With export off, application-prod.yml's setting when nothing overrides it, there is no endpoint to scrape. A
     * context of its own: the enclosing instance's MockMvc belongs to the enclosing context, with export on. It
     * names {@link EmbeddedSQL} again because the database customizer reads it from the test class only, not from
     * an enclosing class.
     */
    @Nested
    @EmbeddedSQL
    @TestPropertySource(properties = "management.prometheus.metrics.export.enabled=false")
    class WithExportOff {

        @Autowired
        private MockMvc exportOff;

        @Test
        void anAnonymousScrapeIsStillRefused() throws Exception {
            exportOff.perform(get("/management/prometheus")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = AuthoritiesConstants.ADMIN)
        void anAdminFindsNoEndpoint() throws Exception {
            exportOff.perform(get("/management/prometheus")).andExpect(status().isNotFound());
        }
    }
}
