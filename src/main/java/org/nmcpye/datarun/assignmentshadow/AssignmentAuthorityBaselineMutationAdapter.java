package org.nmcpye.datarun.assignmentshadow;

import jakarta.el.PropertyNotFoundException;
import jakarta.persistence.EntityManager;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.datatemplateprocessor.ReferenceAssignmentScopeGuard;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.activity.repository.ActivityRepository;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.project.Project;
import org.nmcpye.datarun.jpa.project.repository.ProjectRepository;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.nmcpye.datarun.jpa.user.User;
import org.nmcpye.datarun.jpa.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class AssignmentAuthorityBaselineMutationAdapter {

    private final AssignmentRepository assignments;
    private final TeamRepository teams;
    private final ActivityRepository activities;
    private final ProjectRepository projects;
    private final OrgUnitRepository orgUnits;
    private final UserRepository users;
    private final ReferenceAssignmentScopeGuard scopeGuard;
    private final EntityManager entityManager;

    public AssignmentAuthorityBaselineMutationAdapter(
        AssignmentRepository assignments,
        TeamRepository teams,
        ActivityRepository activities,
        ProjectRepository projects,
        OrgUnitRepository orgUnits,
        UserRepository users,
        ReferenceAssignmentScopeGuard scopeGuard,
        EntityManager entityManager
    ) {
        this.assignments = assignments;
        this.teams = teams;
        this.activities = activities;
        this.projects = projects;
        this.orgUnits = orgUnits;
        this.users = users;
        this.scopeGuard = scopeGuard;
        this.entityManager = entityManager;
    }

    public Optional<Assignment> findAssignment(Assignment assignment) {
        return findExact(
            "Assignment",
            assignment.getId(),
            assignment.getUid(),
            assignments::findById,
            assignments::findByUid
        );
    }

    public Optional<Team> findTeam(Team team) {
        return findExact(
            "Team",
            team.getId(),
            team.getUid(),
            teams::findById,
            teams::findByUid
        );
    }

    public Optional<Activity> findActivity(Activity activity) {
        return findExact(
            "Activity",
            activity.getId(),
            activity.getUid(),
            activities::findById,
            activities::findByUid
        );
    }

    public Optional<Assignment> prepareAssignmentIdentity(
        Assignment assignment,
        boolean update
    ) {
        return prepareIdentity(
            "Assignment",
            assignment,
            update,
            this::findAssignment,
            assignment::setUid
        );
    }

    public Optional<Team> prepareTeamIdentity(Team team, boolean update) {
        return prepareIdentity("Team", team, update, this::findTeam, team::setUid);
    }

    public Optional<Activity> prepareActivityIdentity(Activity activity, boolean update) {
        return prepareIdentity(
            "Activity",
            activity,
            update,
            this::findActivity,
            activity::setUid
        );
    }

    public void requireValidUid(String type, String uid) {
        if (!CodeGenerator.isValidUid(uid)) {
            throw new AssignmentAuthorityConflictException(type + " UID is invalid");
        }
    }

    public Assignment saveAssignment(Assignment assignment, boolean update) {
        Assignment existing = prepareAssignmentIdentity(assignment, update).orElse(null);
        if (existing != null) {
            assignment.setId(existing.getId());
            assignment.setCreatedBy(existing.getCreatedBy());
            assignment.setIsPersisted();
        }

        Activity activity = assignment.getActivity() == null
            ? null : resolveActivity(assignment.getActivity());
        Team team = assignment.getTeam() == null ? null : resolveTeam(assignment.getTeam());
        OrgUnit orgUnit = assignment.getOrgUnit() == null
            ? null : resolveOrgUnit(assignment.getOrgUnit());
        Assignment parent = assignment.getParent() == null
            ? null : resolveAssignment(assignment.getParent());
        scopeGuard.validateScopeUpdate(existing, activity, orgUnit, assignment.getForms());

        assignment.setActivity(activity);
        assignment.setTeam(team);
        assignment.setOrgUnit(orgUnit);
        assignment.setParent(parent);
        if (!Boolean.TRUE.equals(assignment.getDeleted())) {
            assignment.setDeletedAt(null);
        }
        Assignment saved = assignments.save(assignment);
        flush();
        return saved;
    }

    public void softDeleteAssignment(Assignment assignment) {
        Assignment existing = findAssignment(assignment)
            .orElseThrow(() -> notFound("Assignment", assignment.getUid()));
        existing.setDeleted(Boolean.TRUE);
        existing.setDeletedAt(Instant.now());
        assignments.save(existing);
        flush();
    }

    public Team saveTeam(Team team, boolean update) {
        Team existing = prepareTeamIdentity(team, update).orElse(null);
        if (existing != null) {
            team.setId(existing.getId());
            team.setCreatedBy(existing.getCreatedBy());
            team.setIsPersisted();
        }
        team.setActivity(team.getActivity() == null ? null : resolveActivity(team.getActivity()));
        team.setManagedTeams(resolveManagedTeams(team.getManagedTeams()));
        team.setUsers(resolveUsers(team.getUsers()));
        Team saved = teams.save(team);
        flush();
        return saved;
    }

    public Optional<Team> partialUpdateTeam(Team patch) {
        Optional<Team> found = findTeam(patch);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Team existing = found.get();
        if (patch.getUsers() != null && !patch.getUsers().isEmpty()) {
            existing.setUsers(resolveUsers(patch.getUsers()));
        }
        if (patch.getActivity() != null) {
            existing.setActivity(resolveActivity(patch.getActivity()));
        }
        if (patch.getCode() != null) existing.setCode(patch.getCode());
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getDescription() != null) existing.setDescription(patch.getDescription());
        if (patch.getDisabled() != null) existing.setDisabled(patch.getDisabled());
        if (patch.getCreatedBy() != null) existing.setCreatedBy(patch.getCreatedBy());
        if (patch.getCreatedDate() != null) existing.setCreatedDate(patch.getCreatedDate());
        if (patch.getLastModifiedBy() != null) existing.setLastModifiedBy(patch.getLastModifiedBy());
        if (patch.getLastModifiedDate() != null) existing.setLastModifiedDate(patch.getLastModifiedDate());
        Team saved = teams.save(existing);
        flush();
        return Optional.of(saved);
    }

    public void deleteTeam(Team team) {
        Team existing = findTeam(team).orElseThrow(() -> notFound("Team", team.getUid()));
        teams.delete(existing);
        flush();
    }

    public Activity saveActivity(Activity activity, boolean update) {
        Activity existing = prepareActivityIdentity(activity, update).orElse(null);
        if (existing != null) {
            activity.setId(existing.getId());
            activity.setCreatedBy(existing.getCreatedBy());
            activity.setIsPersisted();
        }
        activity.setProject(resolveProject(activity.getProject()));
        Activity saved = activities.save(activity);
        flush();
        return saved;
    }

    public void deleteActivity(Activity activity) {
        Activity existing = findActivity(activity)
            .orElseThrow(() -> notFound("Activity", activity.getUid()));
        activities.delete(existing);
        flush();
    }

    private Set<Team> resolveManagedTeams(Set<Team> supplied) {
        Set<Team> resolved = new HashSet<>();
        if (supplied != null) {
            supplied.forEach(team -> resolved.add(resolveTeam(team)));
        }
        return resolved;
    }

    private Set<User> resolveUsers(Set<User> supplied) {
        Set<User> resolved = new HashSet<>();
        if (supplied == null) {
            return resolved;
        }
        supplied.forEach(user -> resolved.add(resolveUser(user)));
        return resolved;
    }

    private User resolveUser(User user) {
        return Optional.ofNullable(user.getId()).flatMap(users::findById)
            .or(() -> Optional.ofNullable(user.getUid()).flatMap(users::findByUid))
            .or(() -> Optional.ofNullable(user.getLogin()).flatMap(users::findOneByLogin))
            .orElseThrow(() -> new PropertyNotFoundException("User not found: " + user));
    }

    private Team resolveTeam(Team team) {
        return findTeam(team)
            .or(() -> Optional.ofNullable(team.getCode()).flatMap(code -> {
                String activityUid = team.getActivity() == null ? null : team.getActivity().getUid();
                return teams.findByCodeAndActivityUid(code, activityUid);
            }))
            .orElseThrow(() -> new PropertyNotFoundException("Team not found: " + team));
    }

    private Activity resolveActivity(Activity activity) {
        return findActivity(activity).orElseThrow(() ->
            new IllegalQueryException(new ErrorMessage(
                ErrorCode.E1004,
                "Activity",
                Objects.toString(activity.getUid(), activity.getId())
            ))
        );
    }

    private Project resolveProject(Project project) {
        if (project == null) {
            throw new PropertyNotFoundException("Project not found: null");
        }
        return Optional.ofNullable(project.getId()).flatMap(projects::findById)
            .or(() -> Optional.ofNullable(project.getUid()).flatMap(projects::findByUid))
            .or(() -> Optional.ofNullable(project.getCode()).flatMap(projects::findFirstByCode))
            .orElseThrow(() -> new PropertyNotFoundException("Project not found: " + project));
    }

    private OrgUnit resolveOrgUnit(OrgUnit orgUnit) {
        return Optional.ofNullable(orgUnit.getId()).flatMap(orgUnits::findById)
            .or(() -> Optional.ofNullable(orgUnit.getUid()).flatMap(orgUnits::findByUid))
            .or(() -> Optional.ofNullable(orgUnit.getCode()).flatMap(orgUnits::findByCode))
            .orElseThrow(() -> new PropertyNotFoundException("OrgUnit not found: " + orgUnit));
    }

    private Assignment resolveAssignment(Assignment assignment) {
        return findAssignment(assignment).orElseThrow(() ->
            new PropertyNotFoundException("Parent assignment not found: " + assignment)
        );
    }

    private IllegalQueryException notFound(String type, Object identity) {
        return new IllegalQueryException(new ErrorMessage(ErrorCode.E1004, type, identity));
    }

    private <T extends org.nmcpye.datarun.jpa.common.JpaIdentifiableObject>
    Optional<T> prepareIdentity(
        String type,
        T incoming,
        boolean update,
        Function<T, Optional<T>> finder,
        Consumer<String> uidSetter
    ) {
        String uid = incoming.getUid();
        if (uid != null && !uid.isBlank()) {
            requireValidUid(type, uid);
        }

        Optional<T> existing = finder.apply(incoming);
        if (uid == null || uid.isBlank()) {
            if (existing.isPresent()) {
                uidSetter.accept(existing.get().getUid());
            } else if (update) {
                throw notFound(type, incoming.getId());
            } else {
                uidSetter.accept(CodeGenerator.generateUid());
            }
        }
        if (update && existing.isEmpty()) {
            throw notFound(type, incoming.getUid());
        }
        return existing;
    }

    private <T extends org.nmcpye.datarun.jpa.common.JpaIdentifiableObject>
    Optional<T> findExact(
        String type,
        String id,
        String uid,
        Function<String, Optional<T>> findById,
        Function<String, Optional<T>> findByUid
    ) {
        Optional<T> byId = Optional.ofNullable(id).flatMap(findById);
        Optional<T> byUid = Optional.ofNullable(uid)
            .filter(value -> !value.isBlank())
            .flatMap(findByUid);

        if (id != null && uid != null && !uid.isBlank()) {
            boolean exact = byId.isPresent()
                && byUid.isPresent()
                && Objects.equals(byId.get().getId(), byUid.get().getId());
            boolean entirelyNew = byId.isEmpty() && byUid.isEmpty();
            if (!exact && !entirelyNew) {
                throw new AssignmentAuthorityConflictException(
                    type + " id and UID identify different rows"
                );
            }
        }
        return byId.or(() -> byUid);
    }

    private void flush() {
        entityManager.flush();
    }
}
