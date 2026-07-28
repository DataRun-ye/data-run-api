package org.nmcpye.datarun.assignmentshadow;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.datatemplateprocessor.ReferenceAssignmentFormGate;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.activity.repository.ActivityRepository;
import org.nmcpye.datarun.jpa.activity.service.DefaultActivityService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.mapper.AssignmentWithAccessMapper;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentMaintenanceService;
import org.nmcpye.datarun.jpa.assignment.service.DefaultAssignmentService;
import org.nmcpye.datarun.jpa.common.DefaultIdentifiableObjectManager;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.nmcpye.datarun.jpa.team.service.DefaultTeamService;
import org.springframework.cache.CacheManager;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentAuthorityServiceRoutingTest {

    @Test
    void assignmentMutationsDelegateOnlyToCommandOwner() {
        AssignmentRepository repository = mock(AssignmentRepository.class);
        AssignmentAuthorityCommandService commands = mock(AssignmentAuthorityCommandService.class);
        DefaultAssignmentService service = new DefaultAssignmentService(
            repository,
            mock(UserAccessService.class),
            mock(CacheManager.class),
            mock(AssignmentMaintenanceService.class),
            mock(AssignmentWithAccessMapper.class),
            mock(ReferenceAssignmentFormGate.class),
            commands,
            mock(ReleasedWorkReadAuthority.class)
        );
        Assignment assignment = new Assignment();
        when(commands.saveAssignment(assignment)).thenReturn(assignment);
        when(commands.updateAssignment(assignment)).thenReturn(assignment);
        when(repository.findByUid(assignment.getUid())).thenReturn(Optional.of(assignment));

        assertThat(service.save(assignment)).isSameAs(assignment);
        assertThat(service.saveWithRelations(assignment)).isSameAs(assignment);
        assertThat(service.update(assignment)).isSameAs(assignment);
        service.delete(assignment);
        service.deleteByUid(assignment.getUid());
        service.softDelete(assignment);

        verify(commands, org.mockito.Mockito.times(2)).saveAssignment(assignment);
        verify(commands).updateAssignment(assignment);
        verify(commands, org.mockito.Mockito.times(3)).deleteAssignment(assignment);
    }

    @Test
    void teamMutationsIncludingPartialUpdateDelegateOnlyToCommandOwner() {
        TeamRepository repository = mock(TeamRepository.class);
        AssignmentAuthorityCommandService commands = mock(AssignmentAuthorityCommandService.class);
        DefaultTeamService service = new DefaultTeamService(
            repository,
            mock(CacheManager.class),
            mock(UserAccessService.class),
            commands
        );
        Team team = new Team();
        when(commands.saveTeam(team)).thenReturn(team);
        when(commands.updateTeam(team)).thenReturn(team);
        when(commands.partialUpdateTeam(team)).thenReturn(Optional.of(team));
        when(repository.findByUid(team.getUid())).thenReturn(Optional.of(team));

        assertThat(service.save(team)).isSameAs(team);
        assertThat(service.saveWithRelations(team)).isSameAs(team);
        assertThat(service.update(team)).isSameAs(team);
        assertThat(service.partialUpdate(team)).containsSame(team);
        service.delete(team);
        service.deleteByUid(team.getUid());

        verify(commands, org.mockito.Mockito.times(2)).saveTeam(team);
        verify(commands).updateTeam(team);
        verify(commands).partialUpdateTeam(team);
        verify(commands, org.mockito.Mockito.times(2)).deleteTeam(team);
    }

    @Test
    void activityMutationsDelegateOnlyToCommandOwner() {
        ActivityRepository repository = mock(ActivityRepository.class);
        AssignmentAuthorityCommandService commands = mock(AssignmentAuthorityCommandService.class);
        DefaultActivityService service = new DefaultActivityService(
            repository,
            mock(CacheManager.class),
            mock(UserAccessService.class),
            commands
        );
        Activity activity = new Activity();
        when(commands.saveActivity(activity)).thenReturn(activity);
        when(commands.updateActivity(activity)).thenReturn(activity);
        when(repository.findByUid(activity.getUid())).thenReturn(Optional.of(activity));

        assertThat(service.save(activity)).isSameAs(activity);
        assertThat(service.saveWithRelations(activity)).isSameAs(activity);
        assertThat(service.update(activity)).isSameAs(activity);
        service.delete(activity);
        service.deleteByUid(activity.getUid());

        verify(commands, org.mockito.Mockito.times(2)).saveActivity(activity);
        verify(commands).updateActivity(activity);
        verify(commands, org.mockito.Mockito.times(2)).deleteActivity(activity);
    }

    @Test
    void genericObjectManagerRejectsAuthorityOwnedMutations() {
        DefaultIdentifiableObjectManager manager = new DefaultIdentifiableObjectManager(Set.of());

        assertThatThrownBy(() -> manager.save(new Assignment()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AssignmentAuthorityCommandService");
        assertThatThrownBy(() -> manager.update(new Team()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AssignmentAuthorityCommandService");
        assertThatThrownBy(() -> manager.updateNoAcl(new Activity()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AssignmentAuthorityCommandService");
        assertThatThrownBy(() -> manager.delete(new Assignment()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AssignmentAuthorityCommandService");
    }
}
