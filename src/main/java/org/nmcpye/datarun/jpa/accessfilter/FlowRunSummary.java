package org.nmcpye.datarun.jpa.accessfilter;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * @author Hamza Assada 24/04/2025 (7amza.it@gmail.com)
 */
public interface FlowRunSummary {
    @JsonProperty(value = "id")
    String getUid();

    Set<String> getForms();

    RelSummary getOrgUnit();

    RelSummary getTeam();

    RelSummary getActivity();

    interface RelSummary {
        @JsonProperty(value = "id")
        String getUid();
    }
}
