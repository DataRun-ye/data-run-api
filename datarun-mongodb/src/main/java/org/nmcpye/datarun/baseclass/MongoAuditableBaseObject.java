package org.nmcpye.datarun.baseclass;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.common.jpa.JpaAuditableObject;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Base abstract class for entities which will hold definitions for created, last modified, created by,
 * last modified by attributes.
 */
@JsonIgnoreProperties(value = {"createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate"}, allowGetters = true)
@DiffIgnore
@Getter
@Setter
public abstract class MongoAuditableBaseObject
    implements AuditableObject<String>, Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    protected String id;

    @Size(max = 11)
    @Field("uid")
    protected String uid;

    @CreatedBy
    @Field("createdBy")
    protected String createdBy;

    @CreatedDate
    @Field("createdDate")
    protected Instant createdDate = Instant.now();

    @LastModifiedBy
    @Field("lastModifiedBy")
    private String lastModifiedBy;

    @LastModifiedDate
    @Field("lastModifiedDate")
    private Instant lastModifiedDate = Instant.now();

    public MongoAuditableBaseObject() {
        setAutoFields();
    }

    /**
     * Set auto-generated fields on save or update
     */
    public void setAutoFields() {
        if (getUid() == null || getUid().isEmpty()) {
            setUid(CodeGenerator.generateUid());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JpaAuditableObject that)) return false;
        return Objects.equals(getUid(), that.getUid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUid());
    }
}
