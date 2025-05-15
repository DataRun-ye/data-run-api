package org.nmcpye.datarun.startupmigration.mongo;

import org.nmcpye.datarun.mapper.DataFormTemplateMapper;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.service.FormTemplateVersionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <14-05-2025>
 */
@Component
public class FormTemplateMigration implements CommandLineRunner {
    private final DataFormTemplateRepository templateRepository;
    private final FormTemplateVersionService templateVersionService;
    private final DataFormTemplateMapper dataFormTemplateMapper;

    public FormTemplateMigration(DataFormTemplateRepository templateRepository,
                                 FormTemplateVersionService templateVersionService,
                                 DataFormTemplateMapper dataFormTemplateMapper) {
        this.templateRepository = templateRepository;
        this.templateVersionService = templateVersionService;
        this.dataFormTemplateMapper = dataFormTemplateMapper;
    }

    @Override
    public void run(String... args) {
        final var all = templateRepository.findAll();
        all.stream()
            .map(dataFormTemplateMapper::toDto)
            .forEach(templateVersionService::saveNewVersion);
    }
}

