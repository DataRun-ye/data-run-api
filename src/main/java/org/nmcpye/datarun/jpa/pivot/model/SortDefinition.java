package org.nmcpye.datarun.jpa.pivot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Hamza Assada 23/08/2025 (7amza.it@gmail.com)
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SortDefinition implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public enum SortDirection {
        ASC, DESC
    }

    /**
     * The column (or measure alias) to sort by.
     */
    private String column;

    /**
     * The sorting direction.
     */
    private SortDirection direction;
}
