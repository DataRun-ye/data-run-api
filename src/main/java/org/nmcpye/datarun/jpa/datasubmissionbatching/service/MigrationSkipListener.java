package org.nmcpye.datarun.jpa.datasubmissionbatching.service;

import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

/**
 * Generic SkipListener that records skipped items into migration_errors table.
 * It handles skip in read, process and write phases.
 *
 * @author Hamza Assada 16/08/2025 (7amza.it@gmail.com)
 */
@Component
public class MigrationSkipListener implements SkipListener<Object, Object> {

    private final MigrationErrorService errorService;

    public MigrationSkipListener(MigrationErrorService errorService) {
        this.errorService = errorService;
    }

    @Override
    public void onSkipInRead(Throwable t) {
        // read-level skip: raw exception without an item; optionally store null payload
        errorService.recordError(null, "READ", t);
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        errorService.recordError(item, "WRITE", t);
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        errorService.recordError(item, "PROCESS", t);
    }
}
