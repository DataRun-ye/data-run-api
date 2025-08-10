//package org.nmcpye.datarun.web.rest.v1.audit.dto;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.Getter;
//import lombok.Setter;
//import org.javers.core.metamodel.object.CdoSnapshot;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.Serial;
//import java.io.Serializable;
//import java.time.Instant;
//import java.time.LocalDateTime;
//import java.time.ZoneOffset;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Objects;
//
//@Setter
//@Getter
//public class EntityAuditEvent implements Serializable {
//
//    private static final Logger LOG = LoggerFactory.getLogger(EntityAuditEvent.class);
//
//    @Serial
//    private static final long serialVersionUID = 1L;
//
//    private String id;
//
//    private String entityId;
//
//    private String entityType;
//
//    private String action;
//
//    private String entityValue;
//
//    private Integer commitVersion;
//
//    private String modifiedBy;
//
//    private Instant modifiedDate;
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) {
//            return true;
//        }
//        if (o == null || getClass() != o.getClass()) {
//            return false;
//        }
//        EntityAuditEvent entityAuditEvent = (EntityAuditEvent) o;
//        return Objects.equals(id, entityAuditEvent.id);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hashCode(id);
//    }
//
//    @Override
//    public String toString() {
//        return (
//            "EntityAuditEvent{" +
//            "id=" +
//            id +
//            ", entityId='" +
//            entityId +
//            "'" +
//            ", entityType='" +
//            entityType +
//            "'" +
//            ", action='" +
//            action +
//            "'" +
//            ", entityValue='" +
//            entityValue +
//            "'" +
//            ", commitVersion='" +
//            commitVersion +
//            "'" +
//            ", modifiedBy='" +
//            modifiedBy +
//            "'" +
//            ", modifiedDate='" +
//            modifiedDate +
//            "'" +
//            '}'
//        );
//    }
//
//    public static EntityAuditEvent fromJaversSnapshot(CdoSnapshot snapshot) {
//        EntityAuditEvent entityAuditEvent = new EntityAuditEvent();
//
//        switch (snapshot.getType()) {
//            case INITIAL:
//                entityAuditEvent.setAction("CREATE");
//                break;
//            case UPDATE:
//                entityAuditEvent.setAction("UPDATE");
//                break;
//            case TERMINAL:
//                entityAuditEvent.setAction("DELETE");
//                break;
//        }
//
//        entityAuditEvent.setId(snapshot.getCommitId().toString());
//        entityAuditEvent.setCommitVersion(Math.round(snapshot.getVersion()));
//        entityAuditEvent.setEntityType(snapshot.getManagedType().getName());
//        entityAuditEvent.setEntityId(snapshot.getGlobalId().value().split("/")[1]);
//        entityAuditEvent.setModifiedBy(snapshot.getCommitMetadata().getAuthor());
//
//        if (!snapshot.getState().getPropertyNames().isEmpty()) {
//            final Map<String, Object> map = new HashMap<>();
//            snapshot.getState().mapProperties((key, value) -> map.put(key, value != null ? value.toString() : null));
//
//            try {
//                entityAuditEvent.setEntityValue(new ObjectMapper().writeValueAsString(map));
//            } catch (JsonProcessingException e) {
//                LOG.error("Error while producing entityValue JSON string", e);
//            }
//        }
//        LocalDateTime localTime = snapshot.getCommitMetadata().getCommitDate();
//
//        Instant modifyDate = localTime.toInstant(ZoneOffset.UTC);
//
//        entityAuditEvent.setModifiedDate(modifyDate);
//
//        return entityAuditEvent;
//    }
//}
