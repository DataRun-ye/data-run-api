package org.nmcpye.datarun.importer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Hamza Assada 02/06/2025 <7amza.it@gmail.com>
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractBaseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * "ID", "UID", "UUID" or "CODE".
     * Default is "UID"
     */
    private String keyType = "UID";
    private String keyValue; // value for lookup/update
}
