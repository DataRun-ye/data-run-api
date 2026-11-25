package org.nmcpye.datarun.jpa.datasubmission.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.conflict.ConflictDetection;
import org.nmcpye.datarun.jpa.datasubmission.revision.RevisionContext;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.datasubmission.validation.StoredSubmissionGuard;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersionContext;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateVersionResolver;
import org.nmcpye.datarun.jpa.intake.SubmissionIntake;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.reference.ReferenceSubmissionResolver;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.userdetail.UserFormAccess;
import org.nmcpye.datarun.web.rest.v1.datasubmission.mapper.DataSubmissionUploadV1Mapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The legacy route's access checks against the real grant rules: a grant counts only for the team it was given on,
 * and a supervisor's grant reaches the teams he manages. These cases were the old SubmissionAccessValidator's; the
 * legacy route now runs them in {@link SubmissionUploads}.
 */
class LegacyUploadAccessTest {

    private static final String ASSIGNMENT = "assign00001";
    private static final String TEAM_A = "team0000001";
    private static final String TEAM_B = "team0000002";
    private static final String FORM = "form0000001";
    private static final String VERSION = "version0001";

    private SubmissionUploads uploads;

    @BeforeEach
    void setUp() {
        AssignmentRepository assignments = mock(AssignmentRepository.class);
        when(assignments.findByUid(ASSIGNMENT)).thenReturn(Optional.of(assignment()));

        DataTemplateInstanceDto template = mock(DataTemplateInstanceDto.class);
        when(template.getUid()).thenReturn(FORM);
        when(template.getVersionUid()).thenReturn(VERSION);
        when(template.getVersionNumber()).thenReturn(1);
        TemplateVersionContext context = mock(TemplateVersionContext.class);
        when(context.getTemplate()).thenReturn(template);
        when(context.getElementsByPath()).thenReturn(Map.of());
        TemplateVersionResolver resolver = mock(TemplateVersionResolver.class);
        when(resolver.resolveByUid(FORM, VERSION)).thenReturn(context);

        DataSubmissionService submissionService = mock(DataSubmissionService.class);
        doAnswer(invocation -> {
            DataSubmission saved = invocation.getArgument(0);
            EntitySaveSummaryVM summary = invocation.getArgument(2);
            summary.getCreated().add(saved.getUid());
            return null;
        }).when(submissionService).upsert(any(DataSubmission.class), any(), any(EntitySaveSummaryVM.class),
            any(RevisionContext.class));

        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
            .thenReturn(mock(TransactionStatus.class));

        AssignmentFormAccessService formAccess = new AssignmentFormAccessService();
        uploads = new SubmissionUploads(
            submissionService,
            mock(DataSubmissionUploadV1Mapper.class),
            new ObjectMapper(),
            assignments,
            formAccess,
            resolver,
            mock(ReferenceSubmissionResolver.class),
            transactionManager,
            mock(SubmissionIntake.class),
            new StoredSubmissionGuard(assignments, formAccess),
            mock(SubmissionLock.class),
            conflictDetectionOff());
    }

    private static ConflictDetection conflictDetectionOff() {
        ConflictDetection detection = mock(ConflictDetection.class);
        when(detection.isOn()).thenReturn(false);
        return detection;
    }

    @Test
    void rejectsPermissionGrantedThroughAnotherTeam() {
        EntitySaveSummaryVM summary = upload(worker(TEAM_B));

        assertThat(summary.getFailed()).containsEntry("submit00001",
            "E4122: You may not submit form `form0000001` for assignment `assign00001`");
    }

    @Test
    void acceptsPermissionForTheAssignmentTeamAndForm() {
        EntitySaveSummaryVM summary = upload(worker(TEAM_A));

        assertThat(summary.getCreated()).containsExactly("submit00001");
        assertThat(summary.getFailed()).isEmpty();
    }

    @Test
    void acceptsAManagerWithAGrantOnTheManagedTeamsAssignment() {
        EntitySaveSummaryVM summary = upload(manager(FORM));

        assertThat(summary.getCreated()).containsExactly("submit00001");
    }

    @Test
    void rejectsAManagerWithoutAGrantForTheForm() {
        EntitySaveSummaryVM summary = upload(manager("form0000009"));

        assertThat(summary.getFailed().get("submit00001")).startsWith("E4122: ");
    }

    private EntitySaveSummaryVM upload(CurrentUserDetails user) {
        return uploads.saveLegacy(List.of(submission()), SubmissionActor.of(user));
    }

    /** A user of team A whose grant was given on {@code permissionTeam}. */
    private static CurrentUserDetails worker(String permissionTeam) {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.isSuper()).thenReturn(false);
        when(user.getUsername()).thenReturn("worker");
        when(user.getId()).thenReturn("01JWORKER00000000000000001");
        when(user.getUserTeamsUIDs()).thenReturn(Set.of(TEAM_A));
        when(user.getFormAccess()).thenReturn(List.of(
            UserFormAccess.builder()
                .team(permissionTeam)
                .form(FORM)
                .permissions(Set.of(FormPermission.ADD_SUBMISSIONS))
                .build()));
        return user;
    }

    /** A supervisor whose team manages team A; the snapshot copied his grant to team A. */
    private static CurrentUserDetails manager(String grantedForm) {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.isSuper()).thenReturn(false);
        when(user.getUsername()).thenReturn("supervisor");
        when(user.getId()).thenReturn("01JSUPERVISOR0000000000001");
        when(user.getUserTeamsUIDs()).thenReturn(Set.of(TEAM_B));
        when(user.getManagedTeamsUIDs()).thenReturn(Set.of(TEAM_A));
        when(user.getFormAccess()).thenReturn(List.of(
            UserFormAccess.builder()
                .team(TEAM_A)
                .form(grantedForm)
                .permissions(Set.of(FormPermission.ADD_SUBMISSIONS))
                .build()));
        return user;
    }

    private static Assignment assignment() {
        Team team = new Team();
        team.setUid(TEAM_A);
        team.setCode("TEAM-A");
        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setUid("orgUnit0001");
        orgUnit.setCode("OU-1");
        orgUnit.setName("Org unit 1");
        Activity activity = new Activity();
        activity.setUid("activity001");
        Assignment assignment = new Assignment();
        assignment.setUid(ASSIGNMENT);
        assignment.setTeam(team);
        assignment.setOrgUnit(orgUnit);
        assignment.setActivity(activity);
        assignment.setForms(Set.of(FORM));
        return assignment;
    }

    private static DataSubmission submission() {
        DataSubmission submission = new DataSubmission();
        submission.setUid("submit00001");
        submission.setAssignment(ASSIGNMENT);
        submission.setForm(FORM);
        submission.setFormVersion(VERSION);
        return submission;
    }
}
