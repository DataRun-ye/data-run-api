package org.nmcpye.datarun.drun.mongo.service.submissionmigration;

import org.springframework.boot.CommandLineRunner;

//@Component
public class MigrationRunner implements CommandLineRunner {

    private final DataFormSubmissionMigrationService migrationService;

    public MigrationRunner(DataFormSubmissionMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void run(String... args) throws Exception {
//        migrationService.migrateAndAssignSerialNumbers();
        migrationService.migrateAndAddGroupIndices();
    }
}
