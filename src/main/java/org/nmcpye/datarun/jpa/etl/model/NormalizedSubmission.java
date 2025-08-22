package org.nmcpye.datarun.jpa.etl.model;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;
import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;

import java.util.ArrayList;
import java.util.List;


/**
 * A container for the complete, normalized state of a single submission.
 * This object is the output of the Normalizer and the input for the Persister.
 *
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
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

