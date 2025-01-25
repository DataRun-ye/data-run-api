//package org.nmcpye.datarun.drun.postgres.domain;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import io.hypersistence.utils.hibernate.type.json.JsonType;
//import jakarta.persistence.*;
//import jakarta.validation.constraints.Min;
//import jakarta.validation.constraints.Size;
//import org.hibernate.annotations.Cache;
//import org.hibernate.annotations.CacheConcurrencyStrategy;
//import org.hibernate.annotations.Type;
//import org.nmcpye.datarun.domain.AbstractAuditingEntity;
//import org.nmcpye.datarun.drun.postgres.domain.enumeration.TransactionParty;
//import org.nmcpye.datarun.drun.postgres.domain.enumeration.TransactionType;
//import org.nmcpye.datarun.utils.CodeGenerator;
//import org.springframework.data.domain.Persistable;
//
//import java.time.Instant;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * A MovementTransaction.
// */
//@Entity
//@Table(name = "movement_transaction")
//@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
//@JsonIgnoreProperties(value = {"new"})
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class MovementTransaction extends AbstractAuditingEntity<Long>
//    implements Persistable<Long> {
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
//    @SequenceGenerator(name = "sequenceGenerator")
//    @Column(name = "id")
//    private Long id;
//
//    @Size(max = 16)
//    @Column(name = "uid", length = 16, unique = true)
//    private String uid;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "transaction_type", nullable = false)
//    private TransactionType transactionType;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "source_type")
//    private TransactionParty sourceType;
//
//    @Column(name = "source_id")
//    private Long sourceId;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "destination_type")
//    private TransactionParty destinationType;
//
//    @Column(name = "destination_id")
//    private Long destinationId;
//
//    @Size(max = 11)
//    @Column(name = "resource_id", length = 11)
//    private String resourceId;
//
//    @Min(0)
//    @Column(name = "quantity")
//    Double quantity;
//
//    @Column(name = "transaction_time")
//    Instant transactionTime;
//
//    @Type(JsonType.class)
//    @Column(name = "metadata", columnDefinition = "jsonb")
//    protected Map<String, Object> metadata = new HashMap<>();
//
//    @Transient
//    private boolean isPersisted;
//
//    public MovementTransaction() {
//        if (getUid() == null || getUid().isEmpty()) {
//            setUid(CodeGenerator.generateCode(16));
//        }
//    }
//
//    @Override
//    public Long getId() {
//        return id;
//    }
//
//    @Transient
//    @Override
//    public boolean isNew() {
//        return !this.isPersisted;
//    }
//
//    @Override
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    @Override
//    public String getUid() {
//        return uid;
//    }
//
//    @Override
//    public void setUid(String uid) {
//        this.uid = uid;
//    }
//
//    public TransactionType getTransactionType() {
//        return transactionType;
//    }
//
//    public void setTransactionType(TransactionType transactionType) {
//        this.transactionType = transactionType;
//    }
//
//    public TransactionParty getSourceType() {
//        return sourceType;
//    }
//
//    public void setSourceType(TransactionParty sourceType) {
//        this.sourceType = sourceType;
//    }
//
//    public Long getSourceId() {
//        return sourceId;
//    }
//
//    public void setSourceId(Long sourceId) {
//        this.sourceId = sourceId;
//    }
//
//    public TransactionParty getDestinationType() {
//        return destinationType;
//    }
//
//    public void setDestinationType(TransactionParty destinationType) {
//        this.destinationType = destinationType;
//    }
//
//    public Long getDestinationId() {
//        return destinationId;
//    }
//
//    public void setDestinationId(Long destinationId) {
//        this.destinationId = destinationId;
//    }
//
//    public String getResourceId() {
//        return resourceId;
//    }
//
//    public void setResourceId(String resourceId) {
//        this.resourceId = resourceId;
//    }
//
//    public Double getQuantity() {
//        return quantity;
//    }
//
//    public void setQuantity(Double quantity) {
//        this.quantity = quantity;
//    }
//
//    public Instant getTransactionTime() {
//        return transactionTime;
//    }
//
//    public void setTransactionTime(Instant transactionTime) {
//        this.transactionTime = transactionTime;
//    }
//
//    public Map<String, Object> getMetadata() {
//
//        if (metadata == null) {
//            metadata = new HashMap<>();
//        }
//
//        return metadata;
//    }
//
//    public void setMetadata(Map<String, Object> metadata) {
//        this.metadata = metadata;
//    }
//
//    public boolean isPersisted() {
//        return isPersisted;
//    }
//
//    public void setPersisted(boolean persisted) {
//        isPersisted = persisted;
//    }
//}
