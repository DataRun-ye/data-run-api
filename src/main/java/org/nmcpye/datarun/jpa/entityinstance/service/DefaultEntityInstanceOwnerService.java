//package org.nmcpye.datarun.entityinstance;
//
//import lombok.extern.slf4j.Slf4j;
//import org.nmcpye.datarun.assignmenttype.AssignmentType;
//import org.nmcpye.datarun.assignmenttype.AssignmentTypeService;
//import org.nmcpye.datarun.orgunit.OrgUnitService;
//import org.nmcpye.datarun.entityinstance.repository.EntityInstanceOwnerRepository;
//import org.nmcpye.datarun.orgunit.OrgUnit;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Collections;
//import java.util.List;
//
///**
// * Service Implementation for managing {@link EntityInstanceOwner}.
// */
//@Service
//@Slf4j
//public class DefaultEntityInstanceOwnerService implements EntityInstanceOwnerService {
//
//    private final EntityInstanceOwnerRepository entityInstanceOwnerRepository;
//
//    private final EntityInstanceService trackedEntityInstanceService;
//
//    private final AssignmentTypeService assignmentTypeService;
//
//    private final OrgUnitService orgUnitService;
//
//    public DefaultEntityInstanceOwnerService(EntityInstanceOwnerRepository entityInstanceOwnerRepository, EntityInstanceService trackedEntityInstanceService, AssignmentTypeService assignmentTypeService, OrgUnitService orgUnitService) {
//        this.entityInstanceOwnerRepository = entityInstanceOwnerRepository;
//        this.trackedEntityInstanceService = trackedEntityInstanceService;
//        this.assignmentTypeService = assignmentTypeService;
//        this.orgUnitService = orgUnitService;
//    }
//
//    @Override
//    @Transactional
//    public void createEntityInstanceOwner(String entityInstanceUid, String assignmentTypeUid, String orgUnitUid) {
//        final var entityInstance = trackedEntityInstanceService.findByUid(entityInstanceUid);
//        if (entityInstance.isEmpty()) {
//            return;
//        }
//        final var assignmentType = assignmentTypeService.findByUid(assignmentTypeUid);
//
//        if (assignmentType.isEmpty()) {
//            return;
//        }
//
//        final var ou = orgUnitService.findByUid(orgUnitUid);
//        if (ou.isEmpty()) {
//            return;
//        }
//
//        entityInstanceOwnerRepository.save(buildEntityInstanceOwner(entityInstance.get(), assignmentType.get(), ou.get()));
//    }
//
//    @Override
//    @Transactional
//    public void createEntityInstanceOwner(EntityInstance entityInstance, AssignmentType assignmentType, OrgUnit ou) {
//        if (entityInstance == null || assignmentType == null || ou == null) {
//            return;
//        }
//        entityInstanceOwnerRepository.save(buildEntityInstanceOwner(entityInstance, assignmentType, ou));
//    }
//
//    private EntityInstanceOwner buildEntityInstanceOwner(
//            EntityInstance entityInstance,
//            AssignmentType assignmentType,
//            OrgUnit ou) {
//
//        return new EntityInstanceOwner(entityInstance, assignmentType, ou);
//    }
//
//    @Override
//    @Transactional
//    public void createOrUpdateEntityInstanceOwner(String entityInstanceUid, String assignmentTypeUid, String orgUnitUid) {
//        final var entityInstance = trackedEntityInstanceService.findByUid(entityInstanceUid).orElse(null);
//        final var assignmentType = assignmentTypeService.findByUid(assignmentTypeUid).orElse(null);
//        if (entityInstance == null || assignmentType == null) {
//            return;
//        }
//        EntityInstanceOwner entityInstanceOwner = entityInstanceOwnerRepository.findByEntityInstanceIdAndAssignmentTypeId(
//                entityInstance.getId(),
//                assignmentType.getId()
//        ).orElse(null);
//
//        final var ou = orgUnitService.findByUid(orgUnitUid).orElse(null);
//        if (ou == null) {
//            return;
//        }
//
//        if (entityInstanceOwner == null) {
//            entityInstanceOwnerRepository.save(buildEntityInstanceOwner(entityInstance, assignmentType, ou));
//        } else {
//            entityInstanceOwner = updateEntityInstanceOwner(entityInstanceOwner, ou);
//            entityInstanceOwnerRepository.save(entityInstanceOwner);
//        }
//    }
//
//    @Override
//    @Transactional
//    public void createOrUpdateEntityInstanceOwner(Long entityInstanceUid, Long assignmentTypeUid, Long orgUnitUid) {
//        EntityInstance entityInstance = trackedEntityInstanceService.getEntityInstance(entityInstanceUid);
//        AssignmentType assignmentType = assignmentTypeService.getAssignmentType(assignmentTypeUid);
//        if (entityInstance == null) {
//            return;
//        }
//        EntityInstanceOwner entityInstanceOwner = entityInstanceOwnerRepository.getEntityInstanceOwner(
//                entityInstance.getId(),
//                assignmentType.getId()
//        );
//        OrgUnit ou = orgUnitService.getOrgUnit(orgUnitUid);
//        if (ou == null) {
//            return;
//        }
//
//        if (entityInstanceOwner == null) {
//            entityInstanceOwnerRepository.saveObject(buildEntityInstanceOwner(entityInstance, assignmentType, ou));
//        } else {
//            entityInstanceOwner = updateEntityInstanceOwner(entityInstanceOwner, ou);
//            entityInstanceOwnerRepository.update(entityInstanceOwner);
//        }
//    }
//
//    @Override
//    @Transactional
//    public void createOrUpdateEntityInstanceOwner(EntityInstance entityInstance, AssignmentType assignmentType, OrgUnit ou) {
//        if (entityInstance == null || assignmentType == null || ou == null) {
//            return;
//        }
//        EntityInstanceOwner entityInstanceOwner = entityInstanceOwnerRepository.getEntityInstanceOwner(
//                entityInstance.getId(),
//                assignmentType.getId()
//        );
//        if (entityInstanceOwner == null) {
//            entityInstanceOwnerRepository.saveObject(buildEntityInstanceOwner(entityInstance, assignmentType, ou));
//        } else {
//            entityInstanceOwner = updateEntityInstanceOwner(entityInstanceOwner, ou);
//            entityInstanceOwnerRepository.update(entityInstanceOwner);
//        }
//    }
//
//    @Override
//    @Transactional
//    public void updateEntityInstanceOwner(EntityInstance entityInstance, AssignmentType assignmentType, OrgUnit ou) {
//        if (entityInstance == null || assignmentType == null || ou == null) {
//            return;
//        }
//        EntityInstanceOwner entityInstanceOwner = entityInstanceOwnerRepository.getEntityInstanceOwner(
//                entityInstance.getId(),
//                assignmentType.getId()
//        );
//        if (entityInstanceOwner == null) {
//            return;
//        }
//        entityInstanceOwner = updateEntityInstanceOwner(entityInstanceOwner, ou);
//        entityInstanceOwnerRepository.update(entityInstanceOwner);
//    }
//
//    private EntityInstanceOwner updateEntityInstanceOwner(EntityInstanceOwner entityInstanceOwner, OrgUnit ou) {
//        entityInstanceOwner.setOrgUnit(ou);
//        return entityInstanceOwner;
//    }
//
//    @Override
//    @Transactional
//    public void updateEntityInstanceOwner(String entityInstanceUid, String assignmentTypeUid, String orgUnitUid) {
//        EntityInstance entityInstance = trackedEntityInstanceService.getEntityInstance(entityInstanceUid);
//        if (entityInstance == null) {
//            return;
//        }
//        AssignmentType assignmentType = assignmentTypeService.getAssignmentType(assignmentTypeUid);
//        if (assignmentType == null) {
//            return;
//        }
//
//        EntityInstanceOwner teAssignmentTypeOwner = entityInstanceOwnerRepository.getEntityInstanceOwner(
//                entityInstance.getId(),
//                assignmentType.getId()
//        );
//        if (teAssignmentTypeOwner == null) {
//            return;
//        }
//        OrgUnit ou = orgUnitService.getOrgUnit(orgUnitUid);
//        if (ou == null) {
//            return;
//        }
//        teAssignmentTypeOwner = updateEntityInstanceOwner(teAssignmentTypeOwner, ou);
//        entityInstanceOwnerRepository.update(teAssignmentTypeOwner);
//    }
//
//    @Override
//    @Transactional
//    public void createEntityInstanceOwner(Long entityInstanceId, Long assignmentTypeId, Long orgUnitId) {
//        EntityInstance entityInstance = trackedEntityInstanceService.getEntityInstance(entityInstanceId);
//        if (entityInstance == null) {
//            return;
//        }
//        AssignmentType assignmentType = assignmentTypeService.getAssignmentType(assignmentTypeId);
//        if (assignmentType == null) {
//            return;
//        }
//        OrgUnit ou = orgUnitService.getOrgUnit(orgUnitId);
//        if (ou == null) {
//            return;
//        }
//        entityInstanceOwnerRepository.saveObject(buildEntityInstanceOwner(entityInstance, assignmentType, ou));
//    }
//
//    @Override
//    @Transactional
//    public void updateEntityInstanceOwner(Long entityInstanceId, Long assignmentTypeId, Long orgUnitId) {
//        EntityInstanceOwner teAssignmentTypeOwner = entityInstanceOwnerRepository.getEntityInstanceOwner(entityInstanceId, assignmentTypeId);
//        if (teAssignmentTypeOwner == null) {
//            return;
//        }
//        OrgUnit ou = orgUnitService.getOrgUnit(orgUnitId);
//        if (ou == null) {
//            return;
//        }
//        entityInstanceOwnerRepository.update(updateEntityInstanceOwner(teAssignmentTypeOwner, ou));
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public EntityInstanceOwner getEntityInstanceOwner(Long entityInstanceId, Long assignmentTypeId) {
//        return entityInstanceOwnerRepository.getEntityInstanceOwner(entityInstanceId, assignmentTypeId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public EntityInstanceOwner getEntityInstanceOwner(String entityInstanceUid, String assignmentTypeUid) {
//        EntityInstance entityInstance = trackedEntityInstanceService.getEntityInstance(entityInstanceUid);
//        AssignmentType assignmentType = assignmentTypeService.getAssignmentType(assignmentTypeUid);
//        if (entityInstance == null || assignmentType == null) {
//            return null;
//        }
//        return entityInstanceOwnerRepository.getEntityInstanceOwner(entityInstance.getId(), assignmentType.getId());
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<EntityInstanceOwner> getEntityInstanceOwnersUsingId(List<Long> entityInstanceIds) {
//        return entityInstanceOwnerRepository.getEntityInstanceOwners(entityInstanceIds);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<EntityInstanceOwner> getEntityInstanceOwnersUsingId(List<Long> entityInstanceIds, AssignmentType assignmentType) {
//        return entityInstanceOwnerRepository.getEntityInstanceOwners(entityInstanceIds, assignmentType.getId());
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<EntityInstanceOwnerIds> getEntityInstanceOwnersUidsUsingId(List<Long> entityInstanceIds, AssignmentType assignmentType) {
//        if (entityInstanceIds.isEmpty()) {
//            return Collections.emptyList();
//        }
//        return entityInstanceOwnerRepository.getEntityInstanceOwnersUids(entityInstanceIds, assignmentType.getId());
//    }
//}
