package org.nmcpye.datarun.common.mongo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.common.jpa.JpaAuditableObject;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serial;
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
    implements AuditableObject<String> {

    @Serial
    private static final long serialVersionUID = 1L;

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
