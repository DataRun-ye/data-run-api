package org.nmcpye.datarun.jpa.etl.model;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;
import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;

import java.util.*;


/**
 * Holds the normalized output of extraction.
 *
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
@Getter
@Accessors(chain = true)
public class NormalizedSubmission {
    private final String submissionId;
    private final String templateId;
    private final String assignmentId;

    private final List<ElementDataValue> valueRows = new ArrayList<>();
    private final List<RepeatInstance> repeatInstances = new ArrayList<>();

    // repeatPath -> set of incoming repeat UIDs (for mark-and-sweep of repeat instances)
    private final Map<String, Set<String>> incomingRepeatUids = new HashMap<>();

    /**
     * For multi-selects we need a map keyed by (repeatInstanceId, elementId) that maps to the
     * set of selection identities seen in the submission. The identity should be optionId when available,
     * otherwise fallback to valueText. An empty set indicates the element was present with an empty list (explicit clear).
     */
    public static final class MultiSelectKey {
        public final String repeatInstanceId; // can be null for top-level
        public final String elementId;

        public MultiSelectKey(String repeatInstanceId, String elementId) {
            this.repeatInstanceId = repeatInstanceId;
            this.elementId = elementId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MultiSelectKey that)) return false;
            return Objects.equals(repeatInstanceId, that.repeatInstanceId)
                && Objects.equals(elementId, that.elementId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repeatInstanceId, elementId);
        }

        @Override
        public String toString() {
            return (repeatInstanceId == null ? "<ROOT>" : repeatInstanceId) + ":" + elementId;
        }
    }

    private final Map<MultiSelectKey, Set<String>> incomingMultiSelects = new HashMap<>();

    public NormalizedSubmission(String submissionId, String templateId, String assignmentId) {
        this.submissionId = submissionId;
        this.templateId = templateId;
        this.assignmentId = assignmentId;
    }

    public void addValueRow(ElementDataValue r) {
        valueRows.add(r);
    }

    public void addRepeatInstance(RepeatInstance ri) {
        repeatInstances.add(ri);
    }

    public void addIncomingUid(String repeatPath, String uid) {
        incomingRepeatUids.computeIfAbsent(repeatPath, k -> new HashSet<>()).add(uid);
    }

    /**
     * Record a multi-select selection identity seen in the submission for given repeat instance and element.
     * Identity should be optionId if available, otherwise the textual value.
     */
    public void addMultiSelectIdentity(String repeatInstanceId, String elementId, String identity) {
        MultiSelectKey k = new MultiSelectKey(repeatInstanceId, elementId);
        incomingMultiSelects.computeIfAbsent(k, kk -> new HashSet<>()).add(identity);
    }

    /**
     * Record that a multi-select element was present in the submission for the given repeat and element,
     * but it had an explicit empty list (so the set of identities is intentionally empty).
     * <p>
     * Use this when the submission included the key with an empty list; that means the persister should clear
     * any existing selections for that element.
     */
    public void markMultiSelectExplicitlyEmpty(String repeatInstanceId, String elementId) {
        MultiSelectKey k = new MultiSelectKey(repeatInstanceId, elementId);
        incomingMultiSelects.putIfAbsent(k, new HashSet<>()); // empty set means "explicitly empty"
    }
}

