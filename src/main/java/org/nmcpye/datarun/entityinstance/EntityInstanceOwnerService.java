//package org.nmcpye.datarun.entityinstance;
//
//import org.nmcpye.datarun.assignmenttype.AssignmentType;
//import org.nmcpye.datarun.orgunit.OrgUnit;
//
//import java.util.List;
//
///**
// * Service Interface for managing {@link EntityInstanceOwner}.
// */
//public interface EntityInstanceOwnerService {
//
//    void createEntityInstanceOwner(String eiUid, String assignmentTypeUid, String orgUnitUid);
//
//    /**
//     * Update the owner ou for a tracked entity instance for the given assignmentType.
//     * If no owner previously exist, then this method will fail.
//     *
//     * @param eiUid             The tracked entity instance Uid
//     * @param assignmentTypeUid The assignmentType Uid
//     * @param orgUnitUid        The organisation Unit Uid
//     */
//    void updateEntityInstanceOwner(String eiUid, String assignmentTypeUid, String orgUnitUid);
//
//    /**
//     * Assign an orgUnit as the owner for a tracked entity instance for the
//     * given assignmentType. If another owner already exist then this method would
//     * fail.
//     *
//     * @param eiId             The Id of the tracked entity instance
//     * @param assignmentTypeId The assignmentType Id
//     * @param orgUnitId        The organisation units Id
//     */
//    void createEntityInstanceOwner(Long eiId, Long assignmentTypeId, Long orgUnitId);
//
//    /**
//     * Update the owner ou for a tracked entity instance for the given assignmentType.
//     * If no owner previously exist, then this method will fail.
//     *
//     * @param eiId             The tracked entity instance Id
//     * @param assignmentTypeId The assignmentType Id
//     * @param orgUnitId        The organisation Unit Id
//     */
//    void updateEntityInstanceOwner(Long eiId, Long assignmentTypeId, Long orgUnitId);
//
//    /**
//     * Get the assignmentType owner details for a tracked entity instance.
//     *
//     * @param eiId             The tracked entity instance Id
//     * @param assignmentTypeId The assignmentType Id
//     * @return The EntityInstanceOwner object
//     */
//    EntityInstanceOwner getEntityInstanceOwner(Long eiId, Long assignmentTypeId);
//
//    /**
//     * Get the assignmentType owner details for a tracked entity instance.
//     *
//     * @param eiUid             The tracked entity instance Uid
//     * @param assignmentTypeUid The assignmentType Uid
//     * @return The EntityInstanceOwner object
//     */
//    EntityInstanceOwner getEntityInstanceOwner(String eiUid, String assignmentTypeUid);
//
//    /**
//     * Get the assignmentType owner details for a list of eiIds. Includes all possible
//     * assignmentType
//     *
//     * @param eiIds The list of ei Ids
//     * @return The list of EntityInstanceOwner details
//     */
//    List<EntityInstanceOwner> getEntityInstanceOwnersUsingId(List<Long> eiIds);
//
//    /**
//     * Get the assignmentType owner details for a list of eiIds for a specific assignmentType
//     *
//     * @param eiIds          The list of ei Ids
//     * @param assignmentType The assignmentType
//     * @return The list of EntityInstanceOwner details
//     */
//    List<EntityInstanceOwner> getEntityInstanceOwnersUsingId(List<Long> eiIds, AssignmentType assignmentType);
//
//    List<EntityInstanceOwnerIds> getEntityInstanceOwnersUidsUsingId(List<Long> eiIds, AssignmentType assignmentType);
//
//    /**
//     * Assign an orgUnit as the owner for a tracked entity instance for the
//     * given assignmentType. If another owner already exist then it would be
//     * overwritten.
//     *
//     * @param eiUid
//     * @param assignmentTypeUid
//     * @param orgUnitUid
//     */
//    void createOrUpdateEntityInstanceOwner(String eiUid, String assignmentTypeUid, String orgUnitUid);
//
//    /**
//     * Assign an orgUnit as the owner for a tracked entity instance for the
//     * given assignmentType. If another owner already exist then it would be
//     * overwritten.
//     *
//     * @param eiUid
//     * @param assignmentTypeUid
//     * @param orgUnitUid
//     */
//    void createOrUpdateEntityInstanceOwner(Long eiUid, Long assignmentTypeUid, Long orgUnitUid);
//
//    /**
//     * Assign an orgUnit as the owner for a tracked entity instance for the
//     * given assignmentType. If another owner already exist then it would be
//     * overwritten.
//     *
//     * @param entityInstance
//     * @param assignmentType
//     * @param ou
//     */
//    void createOrUpdateEntityInstanceOwner(EntityInstance entityInstance, AssignmentType assignmentType, OrgUnit ou);
//
//    /**
//     * Update the owner ou for a tracked entity instance for the given assignmentType.
//     * If no owner previously exist, then this method will fail.
//     *
//     * @param entityInstance
//     * @param assignmentType
//     * @param ou
//     */
//    void updateEntityInstanceOwner(EntityInstance entityInstance, AssignmentType assignmentType, OrgUnit ou);
//
//    /**
//     * Create a new assignmentType owner ou for a tracked entity instance. If an owner
//     * previously exist, then this method will fail.
//     *
//     * @param entityInstance
//     * @param assignmentType
//     * @param ou
//     */
//    void createEntityInstanceOwner(EntityInstance entityInstance, AssignmentType assignmentType, OrgUnit ou);
//}
