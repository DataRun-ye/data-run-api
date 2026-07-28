package org.nmcpye.datarun.assignmentshadow;

import io.micrometer.core.instrument.MeterRegistry;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.nmcpye.datarun.assignmentshadow.AssignmentCaptureComparisonCategory.COMPATIBILITY_ONLY_EMPTY_CAPTURE;
import static org.nmcpye.datarun.assignmentshadow.AssignmentCaptureComparisonCategory.EXACT;
import static org.nmcpye.datarun.assignmentshadow.AssignmentCaptureComparisonCategory.SHADOW_UNAVAILABLE;
import static org.nmcpye.datarun.assignmentshadow.AssignmentCaptureComparisonCategory.UNEXPLAINED_BASELINE_SCOPE;
import static org.nmcpye.datarun.assignmentshadow.AssignmentCaptureComparisonCategory.UNEXPLAINED_EVENT_SCOPE;
import static org.nmcpye.datarun.assignmentshadow.AssignmentCaptureEventSnapshot.Status.AVAILABLE;
import static org.nmcpye.datarun.assignmentshadow.AssignmentCaptureShadowResult.ACTIVE_GRANT;
import static org.nmcpye.datarun.assignmentshadow.AssignmentCaptureShadowResult.NO_GRANT;
import static org.nmcpye.datarun.assignmentshadow.AssignmentCaptureShadowResult.REVOKED_HISTORY;

@Component
public class AssignmentCaptureShadowComparator {

    private static final Logger log = LoggerFactory.getLogger(
        AssignmentCaptureShadowComparator.class
    );
    private static final String COMPARISON_METER =
        "datarun.assignment.capture.shadow.comparisons";

    private final AssignmentCaptureEventReadPort eventReader;
    private final BaselineAssignmentCaptureAdapter baseline;
    private final MeterRegistry meterRegistry;

    public AssignmentCaptureShadowComparator(
        AssignmentCaptureEventReadPort eventReader,
        BaselineAssignmentCaptureAdapter baseline,
        MeterRegistry meterRegistry
    ) {
        this.eventReader = eventReader;
        this.baseline = baseline;
        this.meterRegistry = meterRegistry;
    }

    public AssignmentCaptureComparisonReport compareAssignmentList(
        CurrentUserDetails user,
        Collection<Assignment> assignments,
        boolean completeResult
    ) {
        AssignmentCaptureSurface surface =
            AssignmentCaptureSurface.ASSIGNMENT_LIST;
        return compareSafely(
            surface,
            assignments.size(),
            List.of(),
            () -> compareAssignmentScopes(
                surface,
                user,
                assignments,
                completeResult
            )
        );
    }

    public AssignmentCaptureComparisonReport compareAssignmentForms(
        CurrentUserDetails user,
        Collection<Assignment> assignments,
        boolean completeResult
    ) {
        AssignmentCaptureSurface surface =
            AssignmentCaptureSurface.ASSIGNMENT_FORMS;
        return compareSafely(
            surface,
            assignments.size(),
            List.of(),
            () -> compareAssignmentScopes(
                surface,
                user,
                assignments,
                completeResult
            )
        );
    }

    public AssignmentCaptureComparisonReport compareDirectOrgUnits(
        CurrentUserDetails user,
        Collection<Assignment> visibleAssignments
    ) {
        AssignmentCaptureSurface surface = AssignmentCaptureSurface.ORG_UNIT_SYNC;
        return compareSafely(
            surface,
            visibleAssignments.size(),
            List.of(),
            () -> compareDirectOrgUnitsInternal(user, visibleAssignments)
        );
    }

    private AssignmentCaptureComparisonReport compareDirectOrgUnitsInternal(
        CurrentUserDetails user,
        Collection<Assignment> visibleAssignments
    ) {
        AssignmentCaptureSurface surface = AssignmentCaptureSurface.ORG_UNIT_SYNC;
        if (excludedAdministrator(user)) {
            return AssignmentCaptureComparisonReport.skipped(surface);
        }

        AssignmentCaptureEventSnapshot shadow = readAll(user);
        if (unavailable(shadow)) {
            return unavailable(
                surface,
                Math.max(1, visibleAssignments.size()),
                List.of()
            );
        }

        Set<String> baselineOrgUnits = new LinkedHashSet<>();
        Set<String> visibleOrgUnits = new LinkedHashSet<>();
        for (Assignment assignment : visibleAssignments) {
            if (assignment.getOrgUnit() != null
                && assignment.getOrgUnit().getUid() != null) {
                visibleOrgUnits.add(assignment.getOrgUnit().getUid());
            }
            baseline.activeScope(user, assignment)
                .map(ignored -> assignment.getOrgUnit().getUid())
                .ifPresent(baselineOrgUnits::add);
        }

        Set<String> eventOrgUnits = activeGrants(shadow).stream()
            .map(AssignmentCaptureEventGrant::baselineOrgUnitUid)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> compatibilityOrgUnits = new LinkedHashSet<>(visibleOrgUnits);
        compatibilityOrgUnits.removeAll(baselineOrgUnits);

        ReportBuilder report = new ReportBuilder(surface);
        baselineOrgUnits.forEach(orgUnitUid -> report.add(
            eventOrgUnits.contains(orgUnitUid) ? EXACT : UNEXPLAINED_BASELINE_SCOPE
        ));
        compatibilityOrgUnits.stream()
            .filter(orgUnitUid -> !eventOrgUnits.contains(orgUnitUid))
            .forEach(ignored -> report.add(COMPATIBILITY_ONLY_EMPTY_CAPTURE));
        eventOrgUnits.stream()
            .filter(orgUnitUid -> !baselineOrgUnits.contains(orgUnitUid))
            .forEach(ignored -> report.add(UNEXPLAINED_EVENT_SCOPE));
        return finish(report, baselineOrgUnits.size(), eventOrgUnits.size());
    }

    public AssignmentCaptureComparisonReport compareReferenceCatalog(
        CurrentUserDetails user,
        Assignment assignment,
        Collection<String> baselinePermittedForms
    ) {
        AssignmentCaptureSurface surface =
            AssignmentCaptureSurface.REFERENCE_CATALOG;
        return compareSafely(
            surface,
            baselinePermittedForms.size(),
            unavailableResults(baselinePermittedForms.size()),
            () -> compareReferenceCatalogInternal(
                user,
                assignment,
                baselinePermittedForms
            )
        );
    }

    private AssignmentCaptureComparisonReport compareReferenceCatalogInternal(
        CurrentUserDetails user,
        Assignment assignment,
        Collection<String> baselinePermittedForms
    ) {
        AssignmentCaptureSurface surface =
            AssignmentCaptureSurface.REFERENCE_CATALOG;
        if (excludedAdministrator(user)) {
            return AssignmentCaptureComparisonReport.skipped(surface);
        }

        List<String> forms = baselinePermittedForms.stream().sorted().toList();
        AssignmentCaptureEventSnapshot shadow = readAssignments(
            user,
            List.of(assignment.getUid())
        );
        if (unavailable(shadow)) {
            return unavailable(
                surface,
                Math.max(1, forms.size()),
                unavailableResults(forms.size())
            );
        }

        ReportBuilder report = new ReportBuilder(surface);
        List<AssignmentCaptureShadowResult> results = new ArrayList<>();
        for (String formUid : forms) {
            AssignmentCaptureShadowResult result = classify(
                shadow,
                assignment,
                formUid,
                baseline.structuralScope(
                    user,
                    assignment,
                    List.of(formUid)
                )
            );
            results.add(result);
            report.add(
                result == ACTIVE_GRANT
                    ? EXACT
                    : UNEXPLAINED_BASELINE_SCOPE
            );
        }
        report.shadowResults(results);
        return finish(report, forms.size(), activeGrants(shadow).size());
    }

    private AssignmentCaptureComparisonReport compareAssignmentScopes(
        AssignmentCaptureSurface surface,
        CurrentUserDetails user,
        Collection<Assignment> assignments,
        boolean completeResult
    ) {
        if (excludedAdministrator(user)) {
            return AssignmentCaptureComparisonReport.skipped(surface);
        }

        Set<String> assignmentUids = assignments.stream()
            .map(Assignment::getUid)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        AssignmentCaptureEventSnapshot shadow = completeResult
            ? readAll(user)
            : readAssignments(user, assignmentUids);
        if (unavailable(shadow)) {
            return unavailable(
                surface,
                Math.max(1, assignments.size()),
                List.of()
            );
        }

        Map<String, List<AssignmentCaptureEventGrant>> activeByAssignment =
            activeGrants(shadow).stream().collect(Collectors.groupingBy(
                AssignmentCaptureEventGrant::baselineAssignmentUid,
                LinkedHashMap::new,
                Collectors.toList()
            ));
        ReportBuilder report = new ReportBuilder(surface);
        long baselineScopeCount = 0;
        for (Assignment assignment : assignments) {
            Optional<AssignmentCaptureScope> expected =
                baseline.activeScope(user, assignment);
            List<AssignmentCaptureEventGrant> actual = activeByAssignment
                .getOrDefault(assignment.getUid(), List.of());
            if (expected.isEmpty()) {
                report.add(
                    actual.isEmpty()
                        ? COMPATIBILITY_ONLY_EMPTY_CAPTURE
                        : UNEXPLAINED_EVENT_SCOPE
                );
                continue;
            }

            baselineScopeCount++;
            report.add(
                actual.size() == 1
                    && actual.get(0).scope().equals(expected.get())
                    ? EXACT
                    : UNEXPLAINED_BASELINE_SCOPE
            );
        }

        if (completeResult) {
            activeByAssignment.keySet().stream()
                .filter(uid -> !assignmentUids.contains(uid))
                .forEach(ignored -> report.add(UNEXPLAINED_EVENT_SCOPE));
        }
        return finish(report, baselineScopeCount, activeGrants(shadow).size());
    }

    private AssignmentCaptureShadowResult classify(
        AssignmentCaptureEventSnapshot shadow,
        Assignment assignment,
        String formUid,
        Optional<AssignmentCaptureScope> structuralScope
    ) {
        List<AssignmentCaptureEventGrant> history = shadow.grants().stream()
            .filter(grant -> grant.baselineAssignmentUid().equals(
                assignment.getUid()
            ))
            .toList();
        if (structuralScope.isPresent()
            && history.stream().anyMatch(grant ->
                grant.lifecycleState() == AssignmentLifecycleState.ACTIVE
                    && grant.matches(structuralScope.get(), formUid))) {
            return ACTIVE_GRANT;
        }
        return history.isEmpty() ? NO_GRANT : REVOKED_HISTORY;
    }

    private AssignmentCaptureEventSnapshot readAll(CurrentUserDetails user) {
        return safeRead(() -> eventReader.readAllForActor(user.getUid()));
    }

    private AssignmentCaptureEventSnapshot readAssignments(
        CurrentUserDetails user,
        Collection<String> assignmentUids
    ) {
        return safeRead(() ->
            eventReader.readAssignments(user.getUid(), assignmentUids)
        );
    }

    private AssignmentCaptureEventSnapshot safeRead(
        Supplier<AssignmentCaptureEventSnapshot> read
    ) {
        try {
            return read.get();
        } catch (RuntimeException exception) {
            log.warn(
                "assignment_capture_shadow reader_status=failed failure_type={}",
                exception.getClass().getSimpleName()
            );
            return AssignmentCaptureEventSnapshot.unavailable();
        }
    }

    private List<AssignmentCaptureEventGrant> activeGrants(
        AssignmentCaptureEventSnapshot shadow
    ) {
        if (shadow.status() != AVAILABLE) {
            return List.of();
        }
        return shadow.grants().stream()
            .filter(grant ->
                grant.lifecycleState() == AssignmentLifecycleState.ACTIVE)
            .toList();
    }

    private boolean unavailable(AssignmentCaptureEventSnapshot shadow) {
        return shadow.status()
            == AssignmentCaptureEventSnapshot.Status.SHADOW_UNAVAILABLE;
    }

    private boolean excludedAdministrator(CurrentUserDetails user) {
        return user == null || user.isSuper();
    }

    private AssignmentCaptureComparisonReport unavailable(
        AssignmentCaptureSurface surface,
        int count,
        List<AssignmentCaptureShadowResult> results
    ) {
        ReportBuilder report = new ReportBuilder(surface);
        report.add(SHADOW_UNAVAILABLE, count);
        report.shadowResults(results);
        return finish(report, 0, 0);
    }

    private List<AssignmentCaptureShadowResult> unavailableResults(int count) {
        List<AssignmentCaptureShadowResult> results =
            new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            results.add(AssignmentCaptureShadowResult.SHADOW_UNAVAILABLE);
        }
        return results;
    }

    private AssignmentCaptureComparisonReport compareSafely(
        AssignmentCaptureSurface surface,
        int expectedCount,
        List<AssignmentCaptureShadowResult> unavailableResults,
        Supplier<AssignmentCaptureComparisonReport> comparison
    ) {
        try {
            return comparison.get();
        } catch (RuntimeException exception) {
            log.warn(
                "assignment_capture_shadow surface={} comparison_status=failed failure_type={}",
                surface.tag(),
                exception.getClass().getSimpleName()
            );
            try {
                return unavailable(
                    surface,
                    Math.max(1, expectedCount),
                    unavailableResults
                );
            } catch (RuntimeException diagnosticsFailure) {
                return new AssignmentCaptureComparisonReport(
                    surface,
                    Map.of(
                        SHADOW_UNAVAILABLE,
                        (long) Math.max(1, expectedCount)
                    ),
                    unavailableResults
                );
            }
        }
    }

    private AssignmentCaptureComparisonReport finish(
        ReportBuilder report,
        long baselineScopeCount,
        long eventGrantCount
    ) {
        if (report.counts.isEmpty()) {
            report.add(EXACT);
        }
        AssignmentCaptureComparisonReport result = report.build();
        result.categories().forEach((category, count) ->
            meterRegistry.counter(
                COMPARISON_METER,
                "surface",
                result.surface().tag(),
                "result",
                category.name().toLowerCase(Locale.ROOT)
            ).increment(count)
        );

        boolean noteworthy = result.count(UNEXPLAINED_BASELINE_SCOPE) > 0
            || result.count(UNEXPLAINED_EVENT_SCOPE) > 0
            || result.count(SHADOW_UNAVAILABLE) > 0;
        if (noteworthy) {
            log.warn(
                "assignment_capture_shadow surface={} results={} baseline_scopes={} event_active_grants={}",
                result.surface().tag(),
                result.categories(),
                baselineScopeCount,
                eventGrantCount
            );
        } else {
            log.debug(
                "assignment_capture_shadow surface={} results={} baseline_scopes={} event_active_grants={}",
                result.surface().tag(),
                result.categories(),
                baselineScopeCount,
                eventGrantCount
            );
        }
        return result;
    }

    private static final class ReportBuilder {
        private final AssignmentCaptureSurface surface;
        private final EnumMap<AssignmentCaptureComparisonCategory, Long> counts =
            new EnumMap<>(AssignmentCaptureComparisonCategory.class);
        private List<AssignmentCaptureShadowResult> shadowResults = List.of();

        private ReportBuilder(AssignmentCaptureSurface surface) {
            this.surface = surface;
        }

        private void add(AssignmentCaptureComparisonCategory category) {
            add(category, 1);
        }

        private void add(
            AssignmentCaptureComparisonCategory category,
            long count
        ) {
            counts.merge(category, count, Long::sum);
        }

        private void shadowResults(
            List<AssignmentCaptureShadowResult> results
        ) {
            shadowResults = List.copyOf(results);
        }

        private AssignmentCaptureComparisonReport build() {
            return new AssignmentCaptureComparisonReport(
                surface,
                Map.copyOf(counts),
                shadowResults
            );
        }
    }
}
