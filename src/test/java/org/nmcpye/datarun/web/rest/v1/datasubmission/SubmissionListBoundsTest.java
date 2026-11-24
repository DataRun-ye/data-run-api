package org.nmcpye.datarun.web.rest.v1.datasubmission;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommandService;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.authorization.ResourceApiAuthorization;
import org.nmcpye.datarun.web.rest.errors.ExceptionTranslator;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The generic list reads of submissions, bounded for an admin before any query runs (E2060): at most 1,000 per page,
 * a page starting within the first 10,000, never unpaged. A non-admin's read is unchanged.
 */
class SubmissionListBoundsTest {

    private DataSubmissionService submissions;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        submissions = mock(DataSubmissionService.class);
        when(submissions.findAllByUser(any(), any())).thenReturn(Page.empty());
        DataSubmissionResource resource = new DataSubmissionResource(submissions, mock(DataSubmissionRepository.class),
            new ObjectMapper(), mock(SubmissionCommandService.class));
        ResourceApiAuthorization authorization = mock(ResourceApiAuthorization.class);
        when(authorization.canRead(any())).thenReturn(true);
        ReflectionTestUtils.setField(resource, "resourceApiAuthorization", authorization);
        mvc = MockMvcBuilders.standaloneSetup(resource)
            .setControllerAdvice(new ExceptionTranslator(new MockEnvironment()))
            .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anAdminsDeepPageIsRefusedWithoutAQuery() throws Exception {
        as(true);

        refused(get("/api/v1/dataSubmission/objects").param("paged", "true").param("size", "1000").param("page", "61")
            .param("flatten", "false"));
        refused(get("/api/v1/dataSubmission").param("size", "100").param("page", "101"));
        refused(get("/api/v1/dataSubmission/byLastModified").param("size", "500").param("page", "21"));

        verifyNoInteractions(submissions);
    }

    @Test
    void anAdminsOversizedPageIsRefusedWithoutAQuery() throws Exception {
        as(true);

        refused(get("/api/v1/dataSubmission/objects").param("size", "1001"));

        verifyNoInteractions(submissions);
    }

    @Test
    void anAdminsUnpagedListIsRefusedWithoutAQuery() throws Exception {
        as(true);

        refused(get("/api/v1/dataSubmission/objects").param("paged", "false"));
        refused(get("/api/v1/dataSubmission").param("paged", "false"));
        mvc.perform(post("/api/v1/dataSubmission/query").param("paged", "false")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest()); // the route answers its own empty 400 to any failure

        verifyNoInteractions(submissions);
    }

    @Test
    void anAdminsPageWithinTheBoundsIsRead() throws Exception {
        as(true);

        mvc.perform(get("/api/v1/dataSubmission/objects").param("size", "1000").param("page", "10"))
            .andExpect(status().isOk());

        verify(submissions).findAllByUser(any(), any());
    }

    /** The G0 pull: a non-admin's unpaged read still runs and still finds nothing (A2 R1). */
    @Test
    void aNonAdminsUnpagedReadIsUnchanged() throws Exception {
        as(false);

        mvc.perform(get("/api/v1/dataSubmission/objects").param("paged", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));

        verify(submissions).findAllByUser(any(), any());
    }

    /**
     * There is no sort parameter (6.5.5): it was never applied, and only a format check read it, which refused a
     * malformed value (E2015) on this route. A client still sending one is ignored like any unknown parameter.
     */
    @Test
    void aSortParameterIsIgnored() throws Exception {
        as(true);

        mvc.perform(get("/api/v1/dataSubmission/objects").param("sort", "not a sort"))
            .andExpect(status().isOk());

        verify(submissions).findAllByUser(any(), any());
    }

    private void refused(MockHttpServletRequestBuilder request) throws Exception {
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.error_code").value("E2060"))
            .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.startsWith(
                "Deep paging over submissions is not supported")));
    }

    private static void as(boolean admin) {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.isSuper()).thenReturn(admin);
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }
}
