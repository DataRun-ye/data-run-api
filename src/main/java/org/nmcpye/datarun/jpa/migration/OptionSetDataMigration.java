//package org.nmcpye.datarun.jpa.migration;
//
//import org.nmcpye.datarun.datatemplateelement.DataOption;
//import org.nmcpye.datarun.jpa.option.Option;
//import org.nmcpye.datarun.jpa.option.OptionSet;
//import org.nmcpye.datarun.jpa.option.repository.OptionSetRepository;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Comparator;
//import java.util.List;
//
///// @author Hamza Assada
///// @since 01/07/2025
////@Component
//public class OptionSetDataMigration implements ApplicationRunner {
//
//    private final OptionSetRepository optionSetRepo;
//
//    public OptionSetDataMigration(OptionSetRepository optionSetRepo) {
//        this.optionSetRepo = optionSetRepo;
//    }
//
//    @Override
//    @Transactional
//    public void run(ApplicationArguments args) throws Exception {
//        List<OptionSet> allSets = optionSetRepo.findAll();
//        for (OptionSet os : allSets) {
//            migrateOne(os);
//        }
//    }
//
//    private void migrateOne(OptionSet os) {
//        List<DataOption> dataOpts = os.getLegacyOptions();
//        dataOpts.sort(Comparator.comparingInt(DataOption::getOrder));
//
//        os.getOptions().clear();
//
//        for (DataOption d : dataOpts) {
//            Option newOpt = new Option();
////            newOpt.setOptionSet(os);
//            newOpt.setCode(d.getName());
//            newOpt.setName(d.getName());
//            newOpt.setLabel(d.getLabel());
//            // …copy all other DataOption fields here…
//            // Note: do NOT set a sortOrder property on Option—@OrderColumn will handle it.
//            os.addOption(newOpt);
////            os.getOptions().add(newOpt);
//        }
//
//        // 4) Persist: cascade = ALL on optionSetOptions will insert the new Option rows
//        optionSetRepo.save(os);
//    }
//}
