//package org.nmcpye.datarun.importprocessor.metamodels;
//
//import lombok.Getter;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
///**
// * Holds metadata for a single JPA entity.
// *
// * @author Hamza Assada 02/06/2025 (7amza.it@gmail.com)
// */
//public class EntitySchema {
//    @Getter
//    private final Class<?> entityType;
//    private final List<IdentifierField> identifierFields = new ArrayList<>();
//    private final List<UniqueField> uniqueFields = new ArrayList<>();
//    private final List<ForeignKey> foreignKeys = new ArrayList<>();
//
//    public EntitySchema(Class<?> entityType) {
//        this.entityType = entityType;
//    }
//
//    public void addIdentifierField(String name, Class<?> type) {
//        this.identifierFields.add(new IdentifierField(name, type));
//    }
//
//    public void addUniqueField(String name, Class<?> type) {
//        this.uniqueFields.add(new UniqueField(name, type));
//    }
//
//    public void addForeignKey(String localField, Class<?> targetEntity, String joinColumn) {
//        this.foreignKeys.add(new ForeignKey(localField, targetEntity, joinColumn));
//    }
//
//    public List<IdentifierField> getIdentifierFields() {
//        return Collections.unmodifiableList(identifierFields);
//    }
//
//    public List<UniqueField> getUniqueFields() {
//        return Collections.unmodifiableList(uniqueFields);
//    }
//
//    public List<ForeignKey> getForeignKeys() {
//        return Collections.unmodifiableList(foreignKeys);
//    }
//
//    public record IdentifierField(String name, Class<?> type) {
//
//    }
//
//    public record UniqueField(String name, Class<?> type) {
//    }
//
//    public record ForeignKey(String localField, Class<?> targetEntity, String joinColumn) {
//    }
//}
