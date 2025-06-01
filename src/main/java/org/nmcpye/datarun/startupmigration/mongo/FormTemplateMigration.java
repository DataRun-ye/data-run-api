package org.nmcpye.datarun.startupmigration.mongo;

import org.nmcpye.datarun.datatemplateversion.FormTemplateVersionService;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <14-05-2025>
 */
@Component
public class FormTemplateMigration implements CommandLineRunner {
    private final DataFormTemplateRepository templateRepository;
    private final FormTemplateVersionService templateVersionService;

    public FormTemplateMigration(DataFormTemplateRepository templateRepository,
                                 FormTemplateVersionService templateVersionService) {
        this.templateRepository = templateRepository;
        this.templateVersionService = templateVersionService;
    }

    @Override
    public void run(String... args) {
        final var all = templateRepository.findAll();
        all.forEach(templateVersionService::migrateDataFormTemplateVersion);
    }
}

