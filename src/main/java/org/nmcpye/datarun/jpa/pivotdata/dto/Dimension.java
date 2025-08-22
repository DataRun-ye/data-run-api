package org.nmcpye.datarun.jpa.pivotdata.dto;

import org.nmcpye.datarun.jpa.pivotdata.model.DimensionDefinition;

/**
 * @author Hamza Assada 18/08/2025 (7amza.it@gmail.com)
 */
public class Dimension {
    public final String id;
    public final String displayName;
    public final String dataType;
    public final String multiSelectStrategy;
    public final Long cardinalityHint;

    public Dimension(String id, String displayName, String dataType, String multiSelectStrategy, Long cardinalityHint) {
        this.id = id;
        this.displayName = displayName;
        this.dataType = dataType;
        this.multiSelectStrategy = multiSelectStrategy;
        this.cardinalityHint = cardinalityHint;
    }

    public static Dimension from(DimensionDefinition def, Long hint) {
        return new Dimension(def.getId(), def.getDisplayName(), def.getDataType().name(),
            def.getMultiSelectStrategy().name(), hint);
    }
}
