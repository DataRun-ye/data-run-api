//package org.nmcpye.datarun.common.impl;
//
//import org.nmcpye.datarun.common.AuditableObjectService;
//import org.nmcpye.datarun.common.VersionedObject;
//
///**
// * @author Hamza Assada, 24/03/2025
// */
//public interface VersionedObjectSave<T extends VersionedObject<ID>, ID> extends AuditableObjectService<T, ID> {
//    default Integer createOrUpdateVersion(T object) {
//        return findByIdentifyingProperties(object).map(T::getVersion).orElse(0);
//    }
//}
