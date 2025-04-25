package org.nmcpye.datarun.mongo.domain.dataform;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Hamza Assada, 24/04/2025
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
public class FormRef {
    private String formUid;
    private String version;
    private Boolean required;
}

