//package org.nmcpye.datarun.jpa.migration;
//
//import org.nmcpye.datarun.jpa.datatemplate.service.DataTemplateInstanceService;
//import org.nmcpye.datarun.mongo.legacydatatemplate.repository.DataFormTemplateRepository;
//import org.springframework.boot.CommandLineRunner;
//
///**
// * @author Hamza Assada 14/05/2025 (7amza.it@gmail.com)
// */
////@Component
//public class FormTemplateMigration implements CommandLineRunner {
//    private final DataFormTemplateRepository templateRepository;
//    private final DataTemplateInstanceService templateVersionService;
//
//    public FormTemplateMigration(DataFormTemplateRepository templateRepository,
//                                 DataTemplateInstanceService templateVersionService) {
//        this.templateRepository = templateRepository;
//        this.templateVersionService = templateVersionService;
//    }
//
//    @Override
//    public void run(String... args) {
//        final var all = templateRepository.findAll();
//        all.forEach(templateVersionService::migrateDataFormTemplateVersion);
//    }
//}
//
