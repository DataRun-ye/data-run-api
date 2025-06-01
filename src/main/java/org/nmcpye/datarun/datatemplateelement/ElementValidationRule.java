package org.nmcpye.datarun.datatemplateelement;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <30-05-2025>
 */
@Value
@Builder
public class ElementValidationRule {
    /**
     * validation's expression
     */
    String expression;

    /**
     * validation's message when expression is met
     */
    Map<String, String> ValidationMessage;
}
