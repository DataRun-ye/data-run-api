package org.nmcpye.datarun.mongo.service.submissionmigration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MigrationRunner implements CommandLineRunner {

    private final DataFormSubmissionMigrationService migrationService;

    public MigrationRunner(DataFormSubmissionMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void run(String... args) throws Exception {
//        migrationService.migrateAndAssignSerialNumbers();
//        migrationService.migrateAndAddGroupIndices();
    }
}
