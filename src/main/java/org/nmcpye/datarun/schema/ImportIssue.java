package org.nmcpye.datarun.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * structure holding field, message, severity
 *
 * @author Hamza Assada, <7amza.it@gmail.com> <01-05-2025>
 */
@Getter
@Setter
@AllArgsConstructor
public class ImportIssue {
    private String property;
    private String message;
    private Severity severity;
}
