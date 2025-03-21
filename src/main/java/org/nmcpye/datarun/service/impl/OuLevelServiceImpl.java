//package org.nmcpye.datarun.service.impl;
//
//import org.nmcpye.datarun.drun.postgres.domain.OuLevel;
//import org.nmcpye.datarun.repository.OuLevelRepository;
//import org.nmcpye.datarun.service.OuLevelService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.Optional;
//
///**
// * Service Implementation for managing {@link OuLevel}.
// */
//@Service
//@Transactional
//public class OuLevelServiceImpl implements OuLevelService {
//
//    private static final Logger log = LoggerFactory.getLogger(OuLevelServiceImpl.class);
//
//    private final OuLevelRepository ouLevelRepository;
//
//    public OuLevelServiceImpl(OuLevelRepository ouLevelRepository) {
//        this.ouLevelRepository = ouLevelRepository;
//    }
//
//    @Override
//    public OuLevel save(OuLevel ouLevel) {
//        log.debug("Request to save OuLevel : {}", ouLevel);
//        return ouLevelRepository.save(ouLevel);
//    }
//
//    @Override
//    public OuLevel update(OuLevel ouLevel) {
//        log.debug("Request to update OuLevel : {}", ouLevel);
//        return ouLevelRepository.save(ouLevel);
//    }
//
//    @Override
//    public Optional<OuLevel> partialUpdate(OuLevel ouLevel) {
//        log.debug("Request to partially update OuLevel : {}", ouLevel);
//
//        return ouLevelRepository
//            .findById(ouLevel.getId())
//            .map(existingOuLevel -> {
//                if (ouLevel.getUid() != null) {
//                    existingOuLevel.setUid(ouLevel.getUid());
//                }
//                if (ouLevel.getName() != null) {
//                    existingOuLevel.setName(ouLevel.getName());
//                }
//                if (ouLevel.getLevel() != null) {
//                    existingOuLevel.setLevel(ouLevel.getLevel());
//                }
//
//                return existingOuLevel;
//            })
//            .map(ouLevelRepository::save);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<OuLevel> findAll() {
//        log.debug("Request to get all OuLevels");
//        return ouLevelRepository.findAll();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Optional<OuLevel> findOne(Long id) {
//        log.debug("Request to get OuLevel : {}", id);
//        return ouLevelRepository.findById(id);
//    }
//
//    @Override
//    public void delete(Long id) {
//        log.debug("Request to delete OuLevel : {}", id);
//        ouLevelRepository.deleteById(id);
//    }
//}
