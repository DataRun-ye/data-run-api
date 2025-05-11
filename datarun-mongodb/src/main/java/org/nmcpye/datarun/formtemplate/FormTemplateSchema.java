package org.nmcpye.datarun.formtemplate;

import org.springframework.data.mongodb.core.schema.MongoJsonSchema;

public class FormTemplateSchema {
    private String formUid;
    private int version;
    private MongoJsonSchema schema;
    // Getters and Setters
}
