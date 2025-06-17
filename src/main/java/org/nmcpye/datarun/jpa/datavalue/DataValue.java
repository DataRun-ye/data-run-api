//package org.nmcpye.datarun.jpa.datavalue;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fasterxml.jackson.databind.annotation.JsonSerialize;
//import jakarta.persistence.*;
//import jakarta.validation.constraints.Size;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import org.apache.commons.lang3.StringUtils;
//import org.hibernate.annotations.Cache;
//import org.hibernate.annotations.CacheConcurrencyStrategy;
//import org.nmcpye.datarun.jpa.common.JpaAuditable;
//import org.nmcpye.datarun.jpa.common.JpaIdentifiableObject;
//import org.nmcpye.datarun.jpa.common.JpaSoftDeleteObject;
//import org.nmcpye.datarun.jpa.stepinstance.StepInstance;
//
//import java.time.Instant;
//import java.util.regex.Pattern;
//
///**
// * @author Hamza Assada 02/06/2025 <7amza.it@gmail.com>
// */
////@Entity
////@Table(name = "data_value", uniqueConstraints = {
////    @UniqueConstraint(name = "uc_data_value_on_data_element_stage_submission",
////        columnNames = {"stage_submission_id", "data_element_uid"})
////})
////@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
////@Getter
////@Setter
////@NoArgsConstructor
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class DataValue extends JpaAuditable<Long> {
//
//    private static final Pattern ZERO_PATTERN = Pattern.compile("^0(\\.0*)?$");
//
//    public static final String TRUE = "true";
//
//    public static final String FALSE = "false";
//
////    @JsonIgnore
////    @Id
////    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
////    @SequenceGenerator(name = "sequenceGenerator")
////    @Column(name = "id")
////    protected Long id;
//
//    @Id
//    @Column(name = "id", length = 26, updatable = false, nullable = false)
//    private String id;
//
//    @Column(name = "deleted")
//    private Boolean deleted = Boolean.FALSE;
//
//    @Size(max = 50000)
//    @Column(name = "value", length = 50000)
//    private String value;
//
//    @Column(name = "data_element_uid")
//    private String dataElementUid;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "stage_submission_id")
//    @JsonSerialize(contentAs = JpaSoftDeleteObject.class)
//    private StepInstance stepInstance;
//
//    public DataValue(String dataElementUid, StepInstance stepInstance) {
//        this.dataElementUid = dataElementUid;
//        this.stepInstance = stepInstance;
//        this.createdDate = Instant.now();
//        this.lastModifiedDate = Instant.now();
//    }
//
//    public DataValue(String dataElementUid, StepInstance stepInstance, String value) {
//        this.dataElementUid = dataElementUid;
//        this.stepInstance = stepInstance;
//        this.value = value;
//        this.createdDate = Instant.now();
//        this.lastModifiedDate = Instant.now();
//    }
//
//    public DataValue(String dataElementUid, StepInstance stepInstance,
//                     String value, String storedBy, Instant lastModifiedDate, String comment) {
//        this.dataElementUid = dataElementUid;
//        this.stepInstance = stepInstance;
//        this.value = value;
//        this.createdDate = Instant.now();
//        this.lastModifiedDate = lastModifiedDate;
//    }
//
//    @Builder(toBuilder = true)
//    public DataValue(String dataElementUid, StepInstance stepInstance,
//                     String value, Instant lastModifiedDate, boolean deleted) {
//        this.dataElementUid = dataElementUid;
//        this.stepInstance = stepInstance;
//        this.value = value;
//        this.createdDate = Instant.now();
//        this.lastModifiedDate = lastModifiedDate;
//        this.deleted = deleted;
//    }
//
//    public boolean isNullValue() {
//        return StringUtils.trimToNull(value) == null;
//    }
//
//    public void mergeWith(DataValue other) {
//        this.value = other.getValue();
//        this.createdDate = other.getCreatedDate();
//        this.lastModifiedDate = other.getLastModifiedDate();
//        this.deleted = other.getDeleted();
//    }
//
//    @JsonProperty
//    @JsonSerialize(contentAs = JpaIdentifiableObject.class)
//    public StepInstance getStepInstance() {
//        return stepInstance;
//    }
//
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) {
//            return true;
//        }
//
//        if (o == null) {
//            return false;
//        }
//
//        if (!getClass().isAssignableFrom(o.getClass())) {
//            return false;
//        }
//
//        final DataValue other = (DataValue) o;
//
//        return dataElementUid.equals(other.getDataElementUid()) &&
//            stepInstance.equals(other.getStepInstance());
//    }
//
//    @Override
//    public int hashCode() {
//        final int prime = 31;
//        int result = 1;
//
//        result = result * prime + dataElementUid.hashCode();
//        result = result * prime + stepInstance.hashCode();
//
//        return result;
//    }
//
//    @Override
//    public String toString() {
//        return "[Data element: " + dataElementUid +
//            ", dataInstance: " + stepInstance.getUid() +
//            ", value: " + value +
//            ", deleted: " + deleted + "]";
//    }
//
//}
