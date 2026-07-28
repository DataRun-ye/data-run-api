package org.nmcpye.datarun.web.rest.postgres.team;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.repository.UpdateAccessDeniedException;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.nmcpye.datarun.jpa.team.service.TeamService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.authorization.ResourceApiAuthorization;
import org.nmcpye.datarun.web.rest.common.BaseReadWriteResource;
import org.nmcpye.datarun.web.rest.postgres.activity.ActivityResource;
import org.nmcpye.datarun.web.rest.postgres.assignment.AssignmentResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamMutationRouteContractTest {

    @Test
    void canonicalAndCompatibilityPatchAliasesUseOneHandler() throws Exception {
        Method handler = TeamResource.class.getDeclaredMethod(
            "partialUpdateTeam",
            String.class,
            Team.class,
            CurrentUserDetails.class
        );

        assertThat(handler.getAnnotation(PatchMapping.class).value())
            .containsExactly("/{uid}", "/teams/{uid}");
    }

    @Test
    void bothApiAliasesExposeTheInheritedMutationSurface() {
        assertThat(AssignmentResource.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/api/custom/assignments", "/api/v1/assignments");
        assertThat(TeamResource.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/api/custom/teams", "/api/v1/teams");
        assertThat(ActivityResource.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/api/custom/activities", "/api/v1/activities");

        Map<String, Method> inherited = Arrays.stream(BaseReadWriteResource.class
                .getDeclaredMethods())
            .collect(Collectors.toMap(Method::getName, method -> method));
        assertThat(inherited).containsKeys(
            "saveAll",
            "saveOne",
            "saveReturnSaved",
            "updateEntity",
            "deleteByIdUid"
        );
        assertThat(inherited.get("saveAll").getAnnotation(
            org.springframework.web.bind.annotation.PostMapping.class
        ).value()).containsExactly("/bulk");
        assertThat(inherited.get("saveOne").getAnnotation(
            org.springframework.web.bind.annotation.PostMapping.class
        ).value()).isEmpty();
        assertThat(inherited.get("saveReturnSaved").getAnnotation(
            org.springframework.web.bind.annotation.PostMapping.class
        ).value()).containsExactly("/return");
        assertThat(inherited.get("updateEntity").getAnnotation(
            org.springframework.web.bind.annotation.PutMapping.class
        ).value()).containsExactly("/{uid}");
        assertThat(inherited.get("deleteByIdUid").getAnnotation(
            org.springframework.web.bind.annotation.DeleteMapping.class
        ).value()).containsExactly("/{id}");
    }

    @Test
    void ordinaryUserCannotPatchTeamOrRunManualPathMaintenance() {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        ResourceApiAuthorization authorization = mock(ResourceApiAuthorization.class);
        when(authorization.canRead(user)).thenReturn(true);
        when(authorization.canManage(user)).thenReturn(false);

        TeamService teamService = mock(TeamService.class);
        TeamResource teamResource = new TeamResource(teamService, mock(TeamRepository.class));
        ReflectionTestUtils.setField(teamResource, "resourceApiAuthorization", authorization);
        Team patch = new Team();
        patch.setUid("T1000000001");

        assertThatThrownBy(() -> teamResource.partialUpdateTeam(
            patch.getUid(),
            patch,
            user
        )).isInstanceOf(UpdateAccessDeniedException.class);
        verify(teamService, never()).partialUpdate(patch);

        AssignmentService assignmentService = mock(AssignmentService.class);
        AssignmentResource assignmentResource = new AssignmentResource(
            assignmentService,
            mock(AssignmentRepository.class)
        );
        ReflectionTestUtils.setField(
            assignmentResource,
            "resourceApiAuthorization",
            authorization
        );

        assertThatThrownBy(() -> assignmentResource.updatePaths(false, user))
            .isInstanceOf(UpdateAccessDeniedException.class);
        verify(assignmentService, never()).updatePaths();
    }
}
