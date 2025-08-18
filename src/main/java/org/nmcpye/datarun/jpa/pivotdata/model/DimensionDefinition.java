package org.nmcpye.datarun.jpa.pivotdata.model;

import lombok.Getter;

import java.util.List;
import java.util.Optional;

/**
 * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
 */
@Getter
public class DimensionDefinition {

    public enum DataType {STRING, NUMERIC, DATE, BOOLEAN}

    public enum MultiSelectStrategy {EXPLODE, DEDUPE_BY_SUBMISSION, CONCAT}

    private final String id;                 // public id e.g. "assignment.org_unit" or "element.<id>"
    private final String displayName;
    private final DataType dataType;
    private final String sqlExpression;      // SQL expression or column name (for now)
    private final List<String> requiredJoins; // SQL fragments or join names; registry consumer will interpret
    private final MultiSelectStrategy multiSelectStrategy;
    private final Long cardinalityHint;      // optional approximate cardinality

    public DimensionDefinition(String id,
                               String displayName,
                               DataType dataType,
                               String sqlExpression,
                               List<String> requiredJoins,
                               MultiSelectStrategy multiSelectStrategy,
                               Long cardinalityHint) {
        this.id = id;
        this.displayName = displayName;
        this.dataType = dataType;
        this.sqlExpression = sqlExpression;
        this.requiredJoins = requiredJoins == null ? List.of() : requiredJoins;
        this.multiSelectStrategy = multiSelectStrategy == null ? MultiSelectStrategy.EXPLODE : multiSelectStrategy;
        this.cardinalityHint = cardinalityHint;
    }

    public Optional<Long> getCardinalityHint() {
        return Optional.ofNullable(cardinalityHint);
    }
}
