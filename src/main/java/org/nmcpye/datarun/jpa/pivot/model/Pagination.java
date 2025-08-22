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
public class Pagination implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The maximum number of records to return.
     */
    private Integer limit;

    /**
     * The offset from the beginning of the result set.
     */
    private Long offset;
}

