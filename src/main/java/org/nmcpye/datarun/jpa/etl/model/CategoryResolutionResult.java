package org.nmcpye.datarun.jpa.etl.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Immutable resolution result: id + kind (kind describes which domain table).
 *
 * @author Hamza Assada 19/08/2025 (7amza.it@gmail.com)
 */
@Value
@Builder
public class CategoryResolutionResult {
    /**
     * canonical id (ULID)
     */
    String uid;
    String name;
    /**
     * e.g. "team", "org_unit", "activity", "domain_entity", "option"
     */
    String kind;

    /**
     * localized display Labels
     */
    Map<String, String> label;
}
