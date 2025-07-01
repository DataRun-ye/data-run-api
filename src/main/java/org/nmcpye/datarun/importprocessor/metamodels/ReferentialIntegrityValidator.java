//package org.nmcpye.datarun.importprocessor.metamodels;
//
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import org.nmcpye.datarun.schema.ImportIssue;
//import org.nmcpye.datarun.schema.Severity;
//import org.springframework.stereotype.Component;
//
//import java.util.*;
//
///**
// * Validates referential integrity generically using EntitySchema metadata.
// *
// * @author Hamza Assada 02/06/2025 (7amza.it@gmail.com)
// */
//@Component
//public class ReferentialIntegrityValidator {
//
//    private final JpaEntitySchemaRegistry schemaRegistry;
//
//    @PersistenceContext
//    private EntityManager entityManager;
//
//    public ReferentialIntegrityValidator(JpaEntitySchemaRegistry schemaRegistry) {
//        this.schemaRegistry = schemaRegistry;
//    }
//
//    /**
//     * Validate a batch of entities for referential integrity.
//     *
//     * @param entities List of entity instances
//     * @param <T>      Type parameter
//     * @return List of ImportIssue
//     */
//    public <T> List<ImportIssue> validateBatch(List<T> entities) {
//        List<ImportIssue> issues = new ArrayList<>();
//        if (entities.isEmpty()) return issues;
//
//        Class<?> entityType = entities.get(0).getClass();
//        EntitySchema schema = schemaRegistry.getSchema(entityType);
//
//        // Collect all FK values by target
//        Map<Class<?>, Set<Object>> fkValuesByTarget = new HashMap<>();
//        for (T entity : entities) {
//            for (EntitySchema.ForeignKey fk : schema.getForeignKeys()) {
//                Object fkValue = getFieldValue(entity, fk.localField());
//                if (fkValue != null) {
//                    fkValuesByTarget.computeIfAbsent(fk.targetEntity(), k -> new HashSet<>())
//                        .add(fkValue);
//                }
//            }
//        }
//
//        // For each target entity, batch fetch existing IDs
//        Map<Class<?>, Set<Object>> existingByTarget = new HashMap<>();
//        for (Map.Entry<Class<?>, Set<Object>> entry : fkValuesByTarget.entrySet()) {
//            Class<?> target = entry.getKey();
//            Set<Object> ids = entry.getValue();
//            if (!ids.isEmpty()) {
//                List<?> found = entityManager.createQuery(
//                        "SELECT e FROM " + target.getSimpleName() + " e WHERE e.id IN :ids")
//                    .setParameter("ids", ids)
//                    .getResultList();
//                Set<Object> foundIds = new HashSet<>();
//                for (Object obj : found) {
//                    Object id = entityManager.getEntityManagerFactory()
//                        .getPersistenceUnitUtil().getIdentifier(obj);
//                    foundIds.add(id);
//                }
//                existingByTarget.put(target, foundIds);
//            }
//        }
//
//        // Report missing references
//        for (T entity : entities) {
//            for (EntitySchema.ForeignKey fk : schema.getForeignKeys()) {
//                Object fkValue = getFieldValue(entity, fk.localField());
//                if (fkValue != null) {
//                    Set<Object> foundIds = existingByTarget.getOrDefault(fk.targetEntity(), Collections.emptySet());
//                    if (!foundIds.contains(fkValue)) {
//                        issues.add(new ImportIssue(fk.localField(),
//                            "Referenced " + fk.targetEntity().getSimpleName() +
//                                " not found for value " + fkValue, Severity.ERROR));
//                    }
//                }
//            }
//        }
//        return issues;
//    }
//
//    private Object getFieldValue(Object entity, String fieldName) {
//        try {
//            var field = entity.getClass().getDeclaredField(fieldName);
//            field.setAccessible(true);
//            return field.get(entity);
//        } catch (Exception e) {
//            return null;
//        }
//    }
//}
