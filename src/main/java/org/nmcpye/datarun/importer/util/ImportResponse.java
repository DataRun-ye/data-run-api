package org.nmcpye.datarun.importer.util;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * @author Hamza Assada 02/06/2025 (7amza.it@gmail.com)
 */
@Builder
@Value
public class ImportResponse {
    // Getters and setters
    boolean dryRun;
    int totalRows;
    int successfulRows;
    int failedRows;
    @Singular
    List<RowError> errors;

    public static ImportResponse success(boolean dryRun, int totalRows) {
        return ImportResponse.builder()
            .dryRun(dryRun)
            .totalRows(totalRows)
            .successfulRows(totalRows)
            .failedRows(0)
            .errors(null)
            .build();
    }

    public static ImportResponse failure(List<RowError> errors, int totalRows) {
        return ImportResponse.builder()
            .dryRun(true)
            .totalRows(totalRows)
            .successfulRows(totalRows - errors.size())
            .failedRows(errors.size())
            .errors(errors).build();
    }
}
