package org.nmcpye.datarun.assignmentshadow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class LatestAssignmentGrantReader {

    private static final Logger log = LoggerFactory.getLogger(
        LatestAssignmentGrantReader.class
    );

    private final AssignmentCaptureEventReadPort eventReader;
    private final AssignmentShadowCheckpoint checkpoint;

    public LatestAssignmentGrantReader(
        AssignmentCaptureEventReadPort eventReader,
        AssignmentShadowCheckpoint checkpoint
    ) {
        this.eventReader = eventReader;
        this.checkpoint = checkpoint;
    }

    public LatestAssignmentGrantSnapshot readAllForActor(
        String baselineUserUid
    ) {
        return readSafely(() -> eventReader.readAllForActor(baselineUserUid));
    }

    public LatestAssignmentGrantSnapshot readAssignments(
        String baselineUserUid,
        Collection<String> assignmentUids
    ) {
        return readSafely(() ->
            eventReader.readAssignments(baselineUserUid, assignmentUids)
        );
    }

    private LatestAssignmentGrantSnapshot readSafely(
        Supplier<AssignmentCaptureEventSnapshot> read
    ) {
        try {
            if (!checkpoint.existsAndIsExact()) {
                return LatestAssignmentGrantSnapshot.unavailable();
            }
            return normalize(read.get());
        } catch (RuntimeException exception) {
            log.warn(
                "assignment_capture_authority reader_status=failed failure_type={}",
                exception.getClass().getSimpleName()
            );
            return LatestAssignmentGrantSnapshot.unavailable();
        }
    }

    private LatestAssignmentGrantSnapshot normalize(
        AssignmentCaptureEventSnapshot snapshot
    ) {
        if (snapshot == null || snapshot.status() == null) {
            return LatestAssignmentGrantSnapshot.unavailable();
        }
        if (snapshot.status()
            == AssignmentCaptureEventSnapshot.Status.SHADOW_UNAVAILABLE) {
            return LatestAssignmentGrantSnapshot.unavailable();
        }
        if (snapshot.status()
            == AssignmentCaptureEventSnapshot.Status.ACTOR_ALIAS_ABSENT) {
            return snapshot.targetActorId() == null && snapshot.grants().isEmpty()
                ? LatestAssignmentGrantSnapshot.actorAliasAbsent()
                : LatestAssignmentGrantSnapshot.unavailable();
        }
        if (snapshot.targetActorId() == null) {
            return LatestAssignmentGrantSnapshot.unavailable();
        }

        Map<String, List<AssignmentCaptureEventGrant>> historyByAssignment =
            new LinkedHashMap<>();
        for (AssignmentCaptureEventGrant grant : snapshot.grants()) {
            if (!valid(grant, snapshot.targetActorId())) {
                return LatestAssignmentGrantSnapshot.unavailable();
            }
            historyByAssignment.computeIfAbsent(
                grant.baselineAssignmentUid(),
                ignored -> new ArrayList<>()
            ).add(grant);
        }

        Map<String, AssignmentCaptureEventGrant> latest =
            new LinkedHashMap<>();
        for (var entry : historyByAssignment.entrySet()) {
            int highestGeneration = entry.getValue().stream()
                .mapToInt(AssignmentCaptureEventGrant::generation)
                .max()
                .orElseThrow();
            List<AssignmentCaptureEventGrant> highest = entry.getValue().stream()
                .filter(grant -> grant.generation() == highestGeneration)
                .toList();
            if (highest.size() != 1) {
                return LatestAssignmentGrantSnapshot.unavailable();
            }
            latest.put(entry.getKey(), highest.get(0));
        }
        return LatestAssignmentGrantSnapshot.available(latest);
    }

    private boolean valid(
        AssignmentCaptureEventGrant grant,
        UUID targetActorId
    ) {
        return grant != null
            && !isBlank(grant.baselineAssignmentUid())
            && grant.generation() >= 0
            && Objects.equals(grant.targetActorId(), targetActorId)
            && !isBlank(grant.activityUid())
            && grant.orgUnitId() != null
            && !isBlank(grant.baselineOrgUnitUid())
            && grant.formUids() != null
            && !grant.formUids().isEmpty()
            && grant.formUids().stream().noneMatch(this::isBlank)
            && grant.lifecycleState() != null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
