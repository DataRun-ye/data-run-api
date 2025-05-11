package org.nmcpye.datarun.migration;

import org.springframework.boot.CommandLineRunner;

//@Component
public class MigrationRunner implements CommandLineRunner {

    private final SubmissionMaintenanceService migrationService;

    public MigrationRunner(SubmissionMaintenanceService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void run(String... args) throws Exception {
//        migrationService.findAndFixFormDataSerialNumbers();
//        migrationService.findAndFixRepeatItemsIndices();

    }
}
