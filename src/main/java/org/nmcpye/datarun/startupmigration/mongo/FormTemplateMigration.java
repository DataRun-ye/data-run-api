package org.nmcpye.datarun.startupmigration.mongo;

import org.nmcpye.datarun.mapper.SaveDataFormTemplateMapper;
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
    private final SaveDataFormTemplateMapper saveDataFormTemplateMapper;

    public FormTemplateMigration(DataFormTemplateRepository templateRepository,
                                 FormTemplateVersionService templateVersionService,
                                 SaveDataFormTemplateMapper saveDataFormTemplateMapper) {
        this.templateRepository = templateRepository;
        this.templateVersionService = templateVersionService;
        this.saveDataFormTemplateMapper = saveDataFormTemplateMapper;
    }

    @Override
    public void run(String... args) {
        final var all = templateRepository.findAll();
        all.stream()
            .map(saveDataFormTemplateMapper::toDto)
            .forEach(templateVersionService::saveNewVersion);
    }
}

