package org.nmcpye.datarun.web.rest.v1.datasubmission;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommand;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommandService;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.datasubmission.validation.CompositeSubmissionValidator;
import org.nmcpye.datarun.jpa.datasubmission.validation.SubmissionAccessValidator;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateVersionResolver;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.authorization.ResourceApiAuthorization;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReferenceUploadRouteContractTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void referenceUploadIsExplicitlyVersionedOnExistingBulkRoute()
        throws Exception {
        Method method = DataSubmissionResource.class.getMethod(
            "saveVersionedUpload",
            List.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);

        assertArrayEquals(
            new String[]{"/bulk"},
            mapping.value());
        assertArrayEquals(
            new String[]{"referenceVersion=1"},
            mapping.params());
        assertEquals(
            "saveAll",
            DataSubmissionResource.class.getMethod(
                "saveAll",
                List.class).getName());
    }

    @Test
    void oldAndVersionedBulkMappingsRegisterWithoutAmbiguity() {
        DataSubmissionResource resource = resource(mock(SubmissionCommandService.class));

        assertDoesNotThrow(
            () -> MockMvcBuilders.standaloneSetup(resource).build());
    }

    /**
     * Practice has a route of its own, so a server without practice support
     * answers 404 instead of storing practice data. Only that route may roll
     * back; the ordinary route must always store.
     */
    @Test
    void onlyThePracticeRouteUploadsWithoutStoring() throws Exception {
        SubmissionCommandService commands = mock(SubmissionCommandService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(resource(commands)).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                mock(CurrentUserDetails.class), null, List.of()));

        mvc.perform(post("/api/v1/dataSubmission/bulk")
                .param("referenceVersion", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andExpect(status().isOk());
        verify(commands).upload(any(SubmissionCommand.Upload.class));
        verifyNoMoreInteractions(commands);

        mvc.perform(post("/api/v1/dataSubmission/bulk/practice")
                .param("referenceVersion", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andExpect(status().isOk());
        verify(commands).uploadPractice(any(SubmissionCommand.PracticeUpload.class));
        verifyNoMoreInteractions(commands);
    }

    /** Without referenceVersion=1, /bulk is the legacy route (apps before 6.0.2). */
    @Test
    void theUnversionedBulkRouteIsTheLegacyUpload() throws Exception {
        SubmissionCommandService commands = mock(SubmissionCommandService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(resource(commands)).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                mock(CurrentUserDetails.class), null, List.of()));

        mvc.perform(post("/api/v1/dataSubmission/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andExpect(status().isOk());
        verify(commands).uploadLegacy(any(SubmissionCommand.LegacyUpload.class));
        verifyNoMoreInteractions(commands);
    }

    private static DataSubmissionResource resource(
        SubmissionCommandService commands) {
        DataSubmissionResource resource = new DataSubmissionResource(
            mock(DataSubmissionService.class),
            mock(DataSubmissionRepository.class),
            new ObjectMapper(),
            mock(CompositeSubmissionValidator.class),
            mock(SubmissionAccessValidator.class),
            mock(TemplateVersionResolver.class),
            commands);
        ResourceApiAuthorization authorization = mock(ResourceApiAuthorization.class);
        when(authorization.canRead(any())).thenReturn(true);
        ReflectionTestUtils.setField(resource, "resourceApiAuthorization", authorization);
        return resource;
    }
}
