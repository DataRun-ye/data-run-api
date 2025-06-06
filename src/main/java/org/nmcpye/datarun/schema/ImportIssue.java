package org.nmcpye.datarun.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * structure holding field, message, severity
 *
 * @author Hamza Assada 01/05/2025 <7amza.it@gmail.com>
 */
@Getter
@Setter
@AllArgsConstructor
public class ImportIssue {
    private String property;
    private String message;
    private Severity severity;
}
