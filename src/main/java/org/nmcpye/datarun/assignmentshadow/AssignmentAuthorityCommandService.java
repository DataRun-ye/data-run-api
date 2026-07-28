package org.nmcpye.datarun.assignmentshadow;

import org.nmcpye.datarun.assignmentshadow.AssignmentAuthoritySnapshot.CaptureIntent;
import org.nmcpye.datarun.assignmentshadow.AssignmentAuthoritySnapshot.IntentKey;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class AssignmentAuthorityCommandService {

    public static final long ADVISORY_LOCK_KEY = 2_180_049_398_680_906_053L;

    private final JdbcTemplate jdbc;
    private final AssignmentShadowCheckpoint checkpoint;
    private final AssignmentAuthoritySnapshot snapshot;
    private final AssignmentGrantLifecycle lifecycle;
    private final AssignmentAuthorityBaselineMutationAdapter baseline;

    public AssignmentAuthorityCommandService(
        JdbcTemplate jdbc,
        AssignmentShadowCheckpoint checkpoint,
        AssignmentAuthoritySnapshot snapshot,
        AssignmentGrantLifecycle lifecycle,
        AssignmentAuthorityBaselineMutationAdapter baseline
    ) {
        this.jdbc = jdbc;
        this.checkpoint = checkpoint;
        this.snapshot = snapshot;
        this.lifecycle = lifecycle;
        this.baseline = baseline;
    }

    @Transactional
    public Assignment saveAssignment(Assignment assignment) {
        return mutateAssignment(assignment, false);
    }

    @Transactional
    public Assignment updateAssignment(Assignment assignment) {
        return mutateAssignment(assignment, true);
    }

    @Transactional
    public void deleteAssignment(Assignment assignment) {
        lock();
        Assignment existing = baseline.findAssignment(assignment)
            .orElseThrow(() -> new AssignmentAuthorityConflictException(
                "Assignment not found: " + assignment.getUid()
            ));
        Set<String> affected = mutableSet(existing.getUid());
        Map<IntentKey, CaptureIntent> before = prepare(affected);
        baseline.softDeleteAssignment(existing);
        finish(affected, before);
    }

    @Transactional
    public Team saveTeam(Team team) {
        return mutateTeam(team, false);
    }

    @Transactional
    public Team updateTeam(Team team) {
        return mutateTeam(team, true);
    }

    @Transactional
    public Optional<Team> partialUpdateTeam(Team team) {
        lock();
        baseline.requireValidUid("Team", team.getUid());
        Team existing = baseline.findTeam(team).orElse(null);
        Set<String> affected = existing == null
            ? new LinkedHashSet<>() : mutableSet(snapshot.assignmentUidsForTeam(existing.getUid()));
        Map<IntentKey, CaptureIntent> before = prepare(affected);
        Optional<Team> saved = baseline.partialUpdateTeam(team);
        saved.ifPresent(value -> affected.addAll(snapshot.assignmentUidsForTeam(value.getUid())));
        finish(affected, before);
        return saved;
    }

    @Transactional
    public void deleteTeam(Team team) {
        lock();
        Team existing = baseline.findTeam(team)
            .orElseThrow(() -> new AssignmentAuthorityConflictException(
                "Team not found: " + team.getUid()
            ));
        Set<String> affected = mutableSet(snapshot.assignmentUidsForTeam(existing.getUid()));
        Map<IntentKey, CaptureIntent> before = prepare(affected);
        baseline.deleteTeam(existing);
        finish(affected, before);
    }

    @Transactional
    public Activity saveActivity(Activity activity) {
        return mutateActivity(activity, false);
    }

    @Transactional
    public Activity updateActivity(Activity activity) {
        return mutateActivity(activity, true);
    }

    @Transactional
    public void deleteActivity(Activity activity) {
        lock();
        Activity existing = baseline.findActivity(activity)
            .orElseThrow(() -> new AssignmentAuthorityConflictException(
                "Activity not found: " + activity.getUid()
            ));
        Set<String> affected = mutableSet(snapshot.assignmentUidsForActivity(existing.getUid()));
        Map<IntentKey, CaptureIntent> before = prepare(affected);
        baseline.deleteActivity(existing);
        finish(affected, before);
    }

    private Assignment mutateAssignment(Assignment assignment, boolean update) {
        lock();
        Set<String> affected = new LinkedHashSet<>();
        baseline.prepareAssignmentIdentity(assignment, update)
            .map(Assignment::getUid)
            .ifPresent(affected::add);
        affected.add(assignment.getUid());
        Map<IntentKey, CaptureIntent> before = prepare(affected);
        Assignment saved = baseline.saveAssignment(assignment, update);
        affected.add(saved.getUid());
        finish(affected, before);
        return saved;
    }

    private Team mutateTeam(Team team, boolean update) {
        lock();
        Team existing = baseline.prepareTeamIdentity(team, update).orElse(null);
        Set<String> affected = existing == null
            ? new LinkedHashSet<>() : mutableSet(snapshot.assignmentUidsForTeam(existing.getUid()));
        Map<IntentKey, CaptureIntent> before = prepare(affected);
        Team saved = baseline.saveTeam(team, update);
        affected.addAll(snapshot.assignmentUidsForTeam(saved.getUid()));
        finish(affected, before);
        return saved;
    }

    private Activity mutateActivity(Activity activity, boolean update) {
        lock();
        Activity existing = baseline.prepareActivityIdentity(activity, update).orElse(null);
        Set<String> affected = existing == null
            ? new LinkedHashSet<>()
            : mutableSet(snapshot.assignmentUidsForActivity(existing.getUid()));
        Map<IntentKey, CaptureIntent> before = prepare(affected);
        Activity saved = baseline.saveActivity(activity, update);
        affected.addAll(snapshot.assignmentUidsForActivity(saved.getUid()));
        finish(affected, before);
        return saved;
    }

    private Map<IntentKey, CaptureIntent> prepare(Set<String> affected) {
        Map<IntentKey, CaptureIntent> before = snapshot.baseline(affected);
        checkpoint.requireCompleted();
        snapshot.requireParity(affected, before);
        return before;
    }

    private void finish(Set<String> affected, Map<IntentKey, CaptureIntent> before) {
        Map<IntentKey, CaptureIntent> after = snapshot.baseline(affected);
        lifecycle.reconcile(
            before,
            after,
            SecurityUtils.getCurrentUserDetailsOrThrow().getUid()
        );
        snapshot.requireParity(affected, after);
    }

    private void lock() {
        jdbc.execute("SELECT pg_advisory_xact_lock(" + ADVISORY_LOCK_KEY + ")");
    }

    private static <T> LinkedHashSet<T> mutableSet(T value) {
        LinkedHashSet<T> values = new LinkedHashSet<>();
        values.add(value);
        return values;
    }

    private static <T> LinkedHashSet<T> mutableSet(Set<T> values) {
        return new LinkedHashSet<>(values);
    }
}
