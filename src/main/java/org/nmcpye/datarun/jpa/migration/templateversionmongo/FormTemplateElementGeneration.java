package org.nmcpye.datarun.jpa.migration.templateversionmongo;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.datatemplate.service.DataTemplateInstanceService;
import org.nmcpye.datarun.jpa.datatemplate.service.ElementTemplateGenerator;
import org.springframework.boot.CommandLineRunner;

/**
 * @author Hamza Assada
 * @since 14/05/2025
 */
//@Component
@RequiredArgsConstructor
public class FormTemplateElementGeneration implements CommandLineRunner {
    private final DataTemplateInstanceService dataTemplateInstanceService;
    private final ElementTemplateGenerator elementTemplateGenerator;

    @Override
    public void run(String... args) {
        final var all = dataTemplateInstanceService.findAll();
        all.forEach(t ->
            elementTemplateGenerator.generateAndSaveFromParsed(t.getUid(),
                t.getVersionUid(), t.getVersionNumber(),
                t.getFields(), t.getSections()));
    }
}

