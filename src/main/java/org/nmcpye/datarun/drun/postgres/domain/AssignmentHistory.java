//package org.nmcpye.datarun.drun.postgres.domain;
//
//import jakarta.persistence.*;
//import org.nmcpye.datarun.domain.User;
//import org.nmcpye.datarun.drun.postgres.domain.enumeration.TransactionType;
//
//import java.io.Serializable;
//import java.time.Instant;
//
//@Entity
//@Table(name = "assignment_history")
//public class AssignmentHistory implements Serializable {
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
//    @SequenceGenerator(name = "sequenceGenerator")
//    private Long id;
//
//    @Column(name = "transaction_type")
//    @Enumerated(EnumType.STRING)
//    private TransactionType transactionType;
//
//    @Column(name = "entity_type")
//    private String entityType;
//
//    @Column(name = "entity_id")
//    private String entityId;
//
//    @Column(name = "field_name")
//    private String fieldName;
//
//    @Column(name = "old_value")
//    private String oldValue;
//
//    @Column(name = "new_value")
//    private String newValue;
//
//    @ManyToOne
//    @JoinColumn(name = "assignment")
//    private Assignment Assignment;
//
//    @Column(name = "form")
//    private String form;
//
//    @ManyToOne
//    @JoinColumn(name = "team_id")
//    private Team team;
//
//    @ManyToOne
//    @JoinColumn(name = "user_id")
//    private User user;
//
//    @Column(name = "transaction_date")
//    private Instant transactionDate;
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
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
//    public String getEntityType() {
//        return entityType;
//    }
//
//    public void setEntityType(String entityType) {
//        this.entityType = entityType;
//    }
//
//    public String getEntityId() {
//        return entityId;
//    }
//
//    public void setEntityId(String entityId) {
//        this.entityId = entityId;
//    }
//
//    public String getFieldName() {
//        return fieldName;
//    }
//
//    public void setFieldName(String fieldName) {
//        this.fieldName = fieldName;
//    }
//
//    public String getOldValue() {
//        return oldValue;
//    }
//
//    public void setOldValue(String oldValue) {
//        this.oldValue = oldValue;
//    }
//
//    public String getNewValue() {
//        return newValue;
//    }
//
//    public void setNewValue(String newValue) {
//        this.newValue = newValue;
//    }
//
//    public Instant getTransactionDate() {
//        return transactionDate;
//    }
//
//    public void setTransactionDate(Instant transactionDate) {
//        this.transactionDate = transactionDate;
//    }
//
//    public User getUser() {
//        return user;
//    }
//
//    public void setUser(User user) {
//        this.user = user;
//    }
//
//    public Team getTeam() {
//        return team;
//    }
//
//    public void setTeam(Team team) {
//        this.team = team;
//    }
//}
