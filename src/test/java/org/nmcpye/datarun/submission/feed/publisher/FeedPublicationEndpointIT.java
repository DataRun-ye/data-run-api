package org.nmcpye.datarun.submission.feed.publisher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.FORM_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The admin's management route for the first publication and republication ({@link FeedPublicationEndpoint}): only an
 * admin reaches it, as every management route but health and info; it starts the work and answers with counts.
 */
@AutoConfigureMockMvc
@FeedPublisherIntegrationTest
@TestPropertySource(properties = {
    "management.endpoints.web.base-path=/management",
    "management.endpoints.web.exposure.include=health,info,feedpublication"
})
class FeedPublicationEndpointIT {

    private static final String ROUTE = "/management/feedpublication";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EtlOrchestrator orchestrator;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private FeedFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new FeedFixture(jdbc, transactionManager, url, username, password);
        fixture.ensureConfiguration();
        fixture.clean();
    }

    @AfterEach
    void tearDown() {
        fixture.clean();
    }

    @Test
    void anAnonymousCallIsRefused() throws Exception {
        mvc.perform(get(ROUTE)).andExpect(status().isUnauthorized());
        mvc.perform(post(ROUTE).contentType(MediaType.APPLICATION_JSON).content("{\"action\": \"republication\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void aFieldUserIsForbidden() throws Exception {
        mvc.perform(get(ROUTE)).andExpect(status().isForbidden());
        mvc.perform(post(ROUTE).contentType(MediaType.APPLICATION_JSON).content("{\"action\": \"republication\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.ADMIN)
    void anAdminMarksAFormForRepublicationAndReadsWhereTheWorkStands() throws Exception {
        fixture.create(document("one", 1));
        while (orchestrator.runOnce("feed-test", 50).claimed() > 0) {
            // the feed holds the record
        }

        mvc.perform(post(ROUTE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\": \"republication\", \"formId\": \"" + FORM_ID + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.marked").value(1));
        mvc.perform(get(ROUTE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.feedOn").value(true))
            .andExpect(jsonPath("$.republishMarksWaiting").value(1));
        mvc.perform(post(ROUTE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\": \"first-publication\", \"formId\": \"" + FORM_ID + "\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(ROUTE).contentType(MediaType.APPLICATION_JSON).content("{\"action\": \"something\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(ROUTE).contentType(MediaType.APPLICATION_JSON).content("{\"action\": \"first-publication\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstPublication.mark").value(""));
        mvc.perform(post(ROUTE).contentType(MediaType.APPLICATION_JSON).content("{\"action\": \"first-publication\"}"))
            .andExpect(status().isConflict());
    }
}
