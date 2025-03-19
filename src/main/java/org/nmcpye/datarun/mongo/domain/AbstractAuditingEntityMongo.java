package org.nmcpye.datarun.mongo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableEntity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.Instant;

/**
 * Base abstract class for entities which will hold definitions for created, last modified, created by,
 * last modified by attributes.
 */
@JsonIgnoreProperties(value = {"createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate"}, allowGetters = true)
@DiffIgnore
public abstract class AbstractAuditingEntityMongo<T>
    implements IdentifiableEntity<T>, /*Comparable<IdentifiableObject<T>>,*/ Serializable {

    private static final long serialVersionUID = 1L;

    public abstract T getId();

    @CreatedBy
    @Field("createdBy")
    private String createdBy;

    @CreatedDate
    @Field("createdDate")
    private Instant createdDate = Instant.now();

    @LastModifiedBy
    @Field("lastModifiedBy")
    private String lastModifiedBy;

    @LastModifiedDate
    @Field("lastModifiedDate")
    private Instant lastModifiedDate = Instant.now();

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
