package org.nmcpye.datarun.assignmentshadow.bootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.nmcpye.datarun.assignmentshadow.bootstrap.AssignmentShadowBootstrapReport.ItemCount;

final class AssignmentShadowBootstrapMetrics {

    ItemCount actorAliases = new ItemCount(0, 0);
    ItemCount orgUnitAliases = new ItemCount(0, 0);
    ItemCount identities = new ItemCount(0, 0);
    ItemCount events = new ItemCount(0, 0);
    ItemCount activeGrants = new ItemCount(0, 0);
    ItemCount endedGrants = new ItemCount(0, 0);
    long rolesCreated;
    long rolesExisting;
    long retiredRows;
    long disabledRows;
    long noActorRows;
    long emptyFormSetRows;
    long nullScopeRows;
    long malformedRows;
    final List<String> validationSamples = new ArrayList<>();

    void addValidationSample(String assignmentDbId, String userDbId, String reason) {
        if (validationSamples.size() >= 10) {
            return;
        }
        validationSamples.add(
            assignmentDbId + "/" + Objects.toString(userDbId, "<no-actor>") + ": " + reason
        );
    }
}
