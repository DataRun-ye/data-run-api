package org.nmcpye.datarun.mongo.domain.dataform;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.mongo.MongoAuditableBaseObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * A FormTemplate.
 */
@Document(collection = "form_template")
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FormTemplate
    extends MongoAuditableBaseObject {
    @Id
    private String id;

    @Size(max = 11)
    @Field("uid")
    @Indexed(unique = true, name = "form_template_uid_idx")
    private String uid;

    @Field("disabled")
    private Boolean disabled = false;

    @Field("deleted")
    private Boolean deleted = false;

    @Field("currentVersion")
    private Integer currentVersion = 0; // latestVersion

    public FormTemplate() {
        setAutoFields();
    }
}
