package org.nmcpye.datarun.datatemplateelement;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <30-05-2025>
 */
@NoArgsConstructor
@Accessors(fluent = true)
@Setter
@Getter
public class ElementValidationRule {
    /**
     * validation's expression
     */
    private String expression;

    /**
     * validation's message when expression is met
     */
    private Map<String, String> validationMessage;
}
