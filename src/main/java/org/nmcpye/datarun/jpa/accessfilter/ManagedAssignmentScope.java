package org.nmcpye.datarun.jpa.accessfilter;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.assignment.repository.ManagedAssignmentRef;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.userdetail.UserFormAccess;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.nmcpye.datarun.common.enumeration.FormPermission.ADD_SUBMISSIONS;
import static org.nmcpye.datarun.common.enumeration.FormPermission.EDIT_SUBMISSIONS;

/**
 * The assignments a user reaches through the teams his teams manage.
 *
 * <p>A managed team's assignment is in scope only when it carries a form the
 * user may submit there (a grant with add or edit, which comes from his own
 * team), so managers with no such form see nothing new. Only apps that can
 * list such assignments receive them: apps up to 6.1.1 fail to show the whole
 * activity when one arrives.</p>
 */
@Component
@RequiredArgsConstructor
public class ManagedAssignmentScope {
    /** Sent by the app (6.1.0+) with every request, for example {@code 6.1.2+57}. */
    static final String APP_VERSION_HEADER = "X-App-Version";
    /** The first app that lists the assignments of managed teams. */
    private static final int[] FIRST_LISTING_APP = {6, 1, 2};
    private static final Pattern VERSION = Pattern.compile("(\\d{1,4})\\.(\\d{1,4})\\.(\\d{1,4})");

    private final AssignmentRepository assignmentRepository;

    @Transactional(readOnly = true)
    public List<ManagedAssignmentRef> findAccessible(CurrentUserDetails user, boolean includeDisabled) {
        Set<String> managedTeams = user.getManagedTeamsUIDs();
        if (user.isSuper() || managedTeams == null || managedTeams.isEmpty()
            || !listsManagedAssignments(currentAppVersion())) {
            return List.of();
        }

        List<ManagedAssignmentRef> scope = new ArrayList<>();
        teamsBySubmittableForms(user, managedTeams).forEach((forms, teams) ->
            scope.addAll(assignmentRepository.findManagedScope(teams, forms, includeDisabled)));
        return scope;
    }

    /** Managed teams grouped by the forms the user may submit on their assignments. */
    private Map<Set<String>, Set<String>> teamsBySubmittableForms(
        CurrentUserDetails user,
        Set<String> managedTeams
    ) {
        Instant now = Instant.now();
        Map<String, Set<String>> formsByTeam = new HashMap<>();
        for (UserFormAccess access : user.getFormAccess() == null ? List.<UserFormAccess>of() : user.getFormAccess()) {
            if (managedTeams.contains(access.getTeam())
                && access.getPermissions() != null
                && CollectionUtils.containsAny(access.getPermissions(), ADD_SUBMISSIONS, EDIT_SUBMISSIONS)
                && (access.getValidFrom() == null || access.getValidFrom().isBefore(now))
                && (access.getValidTo() == null || access.getValidTo().isAfter(now))) {
                formsByTeam.computeIfAbsent(access.getTeam(), team -> new HashSet<>()).add(access.getForm());
            }
        }
        Map<Set<String>, Set<String>> teamsByForms = new HashMap<>();
        formsByTeam.forEach((team, forms) ->
            teamsByForms.computeIfAbsent(forms, key -> new HashSet<>()).add(team));
        return teamsByForms;
    }

    static boolean listsManagedAssignments(String appVersion) {
        if (appVersion == null) {
            return false;
        }
        Matcher matcher = VERSION.matcher(appVersion.trim());
        if (!matcher.lookingAt()) {
            return false;
        }
        int[] version = {
            Integer.parseInt(matcher.group(1)),
            Integer.parseInt(matcher.group(2)),
            Integer.parseInt(matcher.group(3))
        };
        return Arrays.compare(version, FIRST_LISTING_APP) >= 0;
    }

    private static String currentAppVersion() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest().getHeader(APP_VERSION_HEADER);
        }
        return null;
    }
}
