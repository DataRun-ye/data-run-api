package org.nmcpye.datarun.importer.util;

import java.util.List;

/**
 * @author Hamza Assada (02-06-2025), <7amza.it@gmail.com>
 */
public class ImportResponse {
    private boolean dryRun;
    private int totalRows;
    private int successfulRows;
    private int failedRows;
    private List<RowError> errors;

    // Getters and setters
    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getSuccessfulRows() {
        return successfulRows;
    }

    public void setSuccessfulRows(int successfulRows) {
        this.successfulRows = successfulRows;
    }

    public int getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(int failedRows) {
        this.failedRows = failedRows;
    }

    public List<RowError> getErrors() {
        return errors;
    }

    public void setErrors(List<RowError> errors) {
        this.errors = errors;
    }

    public static ImportResponse success(boolean dryRun, int totalRows) {
        ImportResponse r = new ImportResponse();
        r.setDryRun(dryRun);
        r.setTotalRows(totalRows);
        r.setSuccessfulRows(totalRows);
        r.setFailedRows(0);
        r.setErrors(null);
        return r;
    }

    public static ImportResponse failure(List<RowError> errors, int totalRows) {
        ImportResponse r = new ImportResponse();
        r.setDryRun(true);
        r.setTotalRows(totalRows);
        r.setSuccessfulRows(totalRows - errors.size());
        r.setFailedRows(errors.size());
        r.setErrors(errors);
        return r;
    }
}
