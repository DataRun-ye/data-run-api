//package org.nmcpye.datarun.drun.mongo.domain;
//
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//import org.nmcpye.datarun.domain.Activity;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.index.Indexed;
//import org.springframework.data.mongodb.core.mapping.Document;
//import org.springframework.data.mongodb.core.mapping.Field;
//
//import java.time.Instant;
//import java.util.Map;
//
//@Document(collection = "dynamicForms")
//public class DynamicForm {
//    @Id
//    private String id;
//
//    private String uid;
//    private String code;
//    private String defaultLocal;
//    private String description;
//    private boolean disabled;
//    private String name;
//    private Map<String, String> label;
//    private Instant createdDate;
//    private Instant lastModifiedDate;
//    private Activity activity;
//
//    @DBRef
//    private List<DynamicFormField> fields;
//
//    @DBRef
//    private List<FormOption> options;
//
//    // Getters and Setters
//}
