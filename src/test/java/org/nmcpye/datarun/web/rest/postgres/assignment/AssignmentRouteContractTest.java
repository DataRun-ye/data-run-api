package org.nmcpye.datarun.web.rest.postgres.assignment;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.common.BaseReadResource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class AssignmentRouteContractTest {

    @Test
    void releasedMobileAssignmentRoutesRemainRegistered() throws Exception {
        RequestMapping root = AssignmentResource.class
            .getAnnotation(RequestMapping.class);
        assertArrayEquals(
            new String[]{"/api/custom/assignments", "/api/v1/assignments"},
            root.value());

        Method assignments = BaseReadResource.class.getDeclaredMethod(
            "getAll",
            QueryRequest.class);
        assertArrayEquals(
            new String[]{""},
            assignments.getAnnotation(GetMapping.class).value());

        Method forms = AssignmentResource.class.getDeclaredMethod(
            "getAllDto",
            QueryRequest.class,
            String.class,
            int.class,
            CurrentUserDetails.class);
        RequestMapping formsMapping = forms.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"forms"}, formsMapping.value());
        assertArrayEquals(
            new RequestMethod[]{RequestMethod.GET, RequestMethod.POST},
            formsMapping.method());
    }

    @Test
    void assignmentMappingsRegisterWithoutAmbiguity() {
        AssignmentResource resource = new AssignmentResource(
            mock(AssignmentService.class),
            mock(AssignmentRepository.class));

        assertDoesNotThrow(
            () -> MockMvcBuilders.standaloneSetup(resource).build());
    }
}
