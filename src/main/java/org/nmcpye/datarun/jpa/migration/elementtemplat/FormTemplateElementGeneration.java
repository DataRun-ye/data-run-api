package org.nmcpye.datarun.jpa.migration.elementtemplat;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.datatemplate.service.DataTemplateInstanceService;
import org.nmcpye.datarun.jpa.datatemplategenerator.TemplateElementGeneratorService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Hamza Assada
 * @since 14/05/2025
 */
@Component
@Order(1200)
@RequiredArgsConstructor
public class FormTemplateElementGeneration implements CommandLineRunner {
    private final DataTemplateInstanceService dataTemplateInstanceService;
    private final TemplateElementGeneratorService generatorService;

    @Override
    public void run(String... args) {
        final var uids = List.of("YLcsWJlB7uy", "hcLU8ZtUdSd", "Ls9TW9eSlYH", "ck2pHW93sk2",
            "rkwH5QNofRn", "KcsA3KETRbY", "woOl5yAmi8C", "BoEmHvJUEpb",
            "W8EtCDYfp1w", "MrxGOZ6stwS", "UYBPOIfCU0a", "mpmjpJcMuas", "IP2dMtoJkO4",
            "ONIaOpzoYAe", "LaGeMmmCEtH", "zglED4TsbTh", "M3fdtzBSpn8", "RQlMiMcukid");

        final var all = dataTemplateInstanceService.findAllByUidIn(uids);
        all.forEach(t ->
            generatorService.generate(t.getUid(),
                t.getVersionUid()/*, t.getVersionNumber(),
                t.getFields(), t.getSections()*/));
    }
}

