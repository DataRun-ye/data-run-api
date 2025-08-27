package org.nmcpye.datarun.jpa.etl.model;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;
import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Container holding the complete, normalized representation of a single submission.
 *
 * <p>Produced by {@code Normalizer} and consumed by persisters/DAOs.
 * Contains:
 * <ul>
 *   <li>submission identifiers and context</li>
 *   <li>a list of RepeatInstance DTOs (repeat sections)</li>
 *   <li>a list of ElementDataValue rows (normalized element values)</li>
 * </ul>
 *
 * <p>Design notes:
 * <ul>
 *   <li>Mutable lists for incremental population during normalization.</li>
 *   <li>Lightweight: does not itself persist anything; intended as an in-memory transfer object.</li>
 * </ul>
 *
 * @author Hamza Assada
 * @since 13/08/2025
 */
@Getter
@Accessors(chain = true)
public class NormalizedSubmission {
    private final String submissionId;
    private final String templateId; // Still useful for context, but not persisted on value rows
    private final String assignmentId;

    private final List<RepeatInstance> repeatInstances = new ArrayList<>();
    private final List<ElementDataValue> values = new ArrayList<>();

    public NormalizedSubmission(String submissionId, String templateId, String assignmentId) {
        this.submissionId = submissionId;
        this.templateId = templateId;
        this.assignmentId = assignmentId;
    }

    public void addRepeatInstance(RepeatInstance instance) {
        this.repeatInstances.add(instance);
    }

    public void addValueRow(ElementDataValue value) {
        this.values.add(value);
    }
}
