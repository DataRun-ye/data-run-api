//package org.nmcpye.datarun.importprocessor.metamodels;
//
//import jakarta.annotation.PostConstruct;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import jakarta.persistence.metamodel.Attribute;
//import jakarta.persistence.metamodel.EntityType;
//import jakarta.persistence.metamodel.Metamodel;
//import jakarta.persistence.metamodel.SingularAttribute;
//import org.springframework.stereotype.Component;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Registry that introspects JPA metamodel at startup
// * and builds EntitySchema objects for each entity.
// *
// * @author Hamza Assada 02/06/2025 <7amza.it@gmail.com>
// */
//@Component
//public class JpaEntitySchemaRegistry {
//
//    @PersistenceContext
//    private EntityManager entityManager;
//
//    private final Map<Class<?>, EntitySchema> schemaMap = new HashMap<>();
//
//    @PostConstruct
//    public void init() {
//        Metamodel metamodel = entityManager.getMetamodel();
//        for (EntityType<?> entityType : metamodel.getEntities()) {
//            Class<?> javaType = entityType.getJavaType();
//            EntitySchema schema = buildSchema(entityType);
//            schemaMap.put(javaType, schema);
//        }
//    }
//
//    private EntitySchema buildSchema(EntityType<?> entityType) {
//        EntitySchema schema = new EntitySchema(entityType.getJavaType());
//
//        // Discover identifier (primary key)
//        for (SingularAttribute<?, ?> attr : entityType.getIdClassAttributes()) {
//            schema.addIdentifierField(attr.getName(), attr.getJavaType());
//        }
//        if (entityType.hasSingleIdAttribute()) {
//            SingularAttribute<?, ?> idAttr = entityType.getId(entityType.getIdType().getJavaType());
//            schema.addIdentifierField(idAttr.getName(), idAttr.getJavaType());
//        }
//
//        // Discover unique fields (@Column(unique=true) or @NaturalId) - placeholder logic
//        for (Attribute<?, ?> attr : entityType.getAttributes()) {
//            // Pseudocode: check for @Column(unique=true) or Hibernate NaturalId annotation
//            if (isUniqueField(attr)) {
//                schema.addUniqueField(attr.getName(), attr.getJavaType());
//            }
//        }
//
//        // Discover relationships
//        entityType.getAttributes().stream()
//            .filter(Attribute::isAssociation)
//            .forEach(a -> {
//                Class<?> targetType = a.getJavaType();
//                String localName = a.getName();
//                // Pseudocode: find join column name via JPA annotations (not shown)
//                String joinColumn = findJoinColumnName(entityType.getJavaType(), localName);
//                schema.addForeignKey(localName, targetType, joinColumn);
//            });
//
//        return schema;
//    }
//
//    public EntitySchema getSchema(Class<?> entityType) {
//        return schemaMap.get(entityType);
//    }
//
//    private boolean isUniqueField(Attribute<?, ?> attr) {
//        // TODO: inspect annotations on attr.getJavaMember() for @Column(unique=true) or @NaturalId
//        return false;
//    }
//
//    private String findJoinColumnName(Class<?> entityClass, String fieldName) {
//        // TODO: use reflection to read @JoinColumn or default naming
//        return fieldName + "_id";
//    }
//}
//
//
