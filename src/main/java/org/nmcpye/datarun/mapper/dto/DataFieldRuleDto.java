package org.nmcpye.datarun.mapper.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;
import org.nmcpye.datarun.mongo.domain.enumeration.RuleAction;

import java.io.Serializable;
import java.util.Map;

/**
 * DTO for {@link org.nmcpye.datarun.mongo.domain.DataFieldRule}
 */
@Value
public class DataFieldRuleDto implements Serializable {
    @NotNull
    String expression;
    @NotNull
    RuleAction action;
    Map<String, String> message;
    String assignedValue;
}
