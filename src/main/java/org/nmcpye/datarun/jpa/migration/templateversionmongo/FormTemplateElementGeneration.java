package org.nmcpye.datarun.jpa.migration.templateversionmongo;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.datatemplate.service.DataTemplateInstanceService;
import org.nmcpye.datarun.jpa.datatemplategenerator.ElementTemplateConfigGeneratorService;
import org.springframework.boot.CommandLineRunner;

/**
 * @author Hamza Assada
 * @since 14/05/2025
 */
//@Component
//@Order(1200)
@RequiredArgsConstructor
public class FormTemplateElementGeneration implements CommandLineRunner {
    private final DataTemplateInstanceService dataTemplateInstanceService;
    private final ElementTemplateConfigGeneratorService generatorService;

    @Override
    public void run(String... args) {
        final var all = dataTemplateInstanceService.findAll();
        all.forEach(t ->
            generatorService.generate(t.getUid(),
                t.getVersionUid()/*, t.getVersionNumber(),
                t.getFields(), t.getSections()*/));
    }
}

