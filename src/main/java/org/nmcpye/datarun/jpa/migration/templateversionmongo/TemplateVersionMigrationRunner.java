package org.nmcpye.datarun.jpa.migration.templateversionmongo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

/**
 * @author Hamza Assada - 7amza.it@gmail.com
 * @since 24/08/2025
 */
@Slf4j
//@Component
@RequiredArgsConstructor
public class TemplateVersionMigrationRunner implements CommandLineRunner {
    private final TemplateVersionMigrationService migrationService;
    private final MigrationProperties props = new MigrationProperties();

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting TemplateVersion migration. props={}", props);
        try {
            migrationService.migrateAll(props);
            log.info("TemplateVersion migration completed successfully.");
        } catch (Exception e) {
            log.error("TemplateVersion migration failed.", e);
            throw e;
        }
    }
}
