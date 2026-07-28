package org.nmcpye.datarun.web.rest.v1.datasubmission;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.datasubmission.validation.CompositeSubmissionValidator;
import org.nmcpye.datarun.jpa.datasubmission.validation.SubmissionAccessValidator;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateVersionResolver;
import org.nmcpye.datarun.web.rest.v1.datasubmission.service.SubmissionUploadService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ReferenceUploadRouteContractTest {

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
        DataSubmissionResource resource = new DataSubmissionResource(
            mock(DataSubmissionService.class),
            mock(DataSubmissionRepository.class),
            new ObjectMapper(),
            mock(CompositeSubmissionValidator.class),
            mock(SubmissionAccessValidator.class),
            mock(TemplateVersionResolver.class),
            mock(SubmissionUploadService.class));

        assertDoesNotThrow(
            () -> MockMvcBuilders.standaloneSetup(resource).build());
    }
}
