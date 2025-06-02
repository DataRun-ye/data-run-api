package org.nmcpye.datarun.jpa.datavalue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.common.JpaAuditable;
import org.nmcpye.datarun.jpa.common.JpaAuditableObject;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.nmcpye.datarun.jpa.datainstance.DataInstance;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * @author Hamza Assada (02-06-2025), <7amza.it@gmail.com>
 */
@Entity
@Table(name = "data_value")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataValue extends JpaAuditable<Long> {

    private static final Pattern ZERO_PATTERN = Pattern.compile("^0(\\.0*)?$");

    public static final String TRUE = "true";

    public static final String FALSE = "false";

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    protected Long id;

    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    @Size(max = 50000)
    @Column(name = "value", length = 50000)
    private String value;

    @Column(name = "followup")
    private Boolean followup = Boolean.FALSE;

    @Column(name = "comment")
    private String comment;

    @Column(name = "stored_by")
    private String storedBy;

    @ManyToOne(optional = false)
    @JoinColumn(name = "data_element_id")
    @JsonIgnoreProperties(value = {"dataElementGroups", "optionSet", "createdDate", "lastModifiedDate", "lastModifiedBy"}, allowSetters = true)
    private DataElement dataElement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "data_instance_id")
    private DataInstance dataInstance;

    public DataValue() {
        this.createdDate = Instant.now();
        this.lastModifiedDate = Instant.now();
    }

    public DataValue(DataElement dataElement, DataInstance dataInstance) {
        this.dataElement = dataElement;
        this.dataInstance = dataInstance;
        this.createdDate = Instant.now();
        this.lastModifiedDate = Instant.now();
    }

    public DataValue(DataElement dataElement, DataInstance dataInstance, String value) {
        this.dataElement = dataElement;
        this.dataInstance = dataInstance;
        this.value = value;
        this.createdDate = Instant.now();
        this.lastModifiedDate = Instant.now();
    }

    public DataValue(DataElement dataElement, DataInstance dataInstance,
                     String value, String storedBy, Instant lastModifiedDate, String comment) {
        this.dataElement = dataElement;
        this.dataInstance = dataInstance;
        this.value = value;
        this.storedBy = storedBy;
        this.createdDate = Instant.now();
        this.lastModifiedDate = lastModifiedDate;
        this.comment = comment;
    }

    @Builder(toBuilder = true)
    public DataValue(DataElement dataElement, DataInstance dataInstance,
                     String value, String storedBy, Instant lastModifiedDate, String comment,
                     Boolean followup, boolean deleted) {
        this.dataElement = dataElement;
        this.dataInstance = dataInstance;
        this.value = value;
        this.storedBy = storedBy;
        this.createdDate = Instant.now();
        this.lastModifiedDate = lastModifiedDate;
        this.comment = comment;
        this.followup = followup;
        this.deleted = deleted;
    }

    public boolean isNullValue() {
        return StringUtils.trimToNull(value) == null && StringUtils.trimToNull(comment) == null;
    }

    public void mergeWith(DataValue other) {
        this.value = other.getValue();
        this.storedBy = other.getStoredBy();
        this.createdDate = other.getCreatedDate();
        this.lastModifiedDate = other.getLastModifiedDate();
        this.comment = other.getComment();
        this.followup = other.getFollowup();
        this.deleted = other.getDeleted();
    }

    @JsonProperty
    @JsonSerialize(contentAs = JpaAuditableObject.class)
    public DataInstance getDataInstance() {
        return dataInstance;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!getClass().isAssignableFrom(o.getClass())) {
            return false;
        }

        final DataValue other = (DataValue) o;

        return dataElement.equals(other.getDataElement()) &&
            dataInstance.equals(other.getDataInstance());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = result * prime + dataElement.hashCode();
        result = result * prime + dataInstance.hashCode();

        return result;
    }

    @Override
    public String toString() {
        return "[Data element: " + dataElement.getUid() +
            ", dataInstance: " + dataInstance.getUid() +
            ", value: " + value +
            ", deleted: " + deleted + "]";
    }

}
