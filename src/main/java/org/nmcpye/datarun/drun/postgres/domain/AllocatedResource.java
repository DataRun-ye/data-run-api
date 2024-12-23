package org.nmcpye.datarun.drun.postgres.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.mongo.domain.enumeration.ResourceType;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "allocated_resource", uniqueConstraints = {
    @UniqueConstraint(name = "uc_assignment_allocated_resource",
        columnNames = {"assignment_id", "resourceType"})
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(value = {"new"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AllocatedResource
    implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "resource_type")
    private ResourceType resourceType;

    @Column(name = "quantity")
    private double quantity;

    @ManyToOne
    @JsonIgnoreProperties(value = {"parent", "children", "activity", "team", "orgUnit", "ancestors", "translations"}, allowSetters = true)
    private Assignment assignment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AllocatedResource that)) return false;
        return Double.compare(quantity, that.quantity) == 0 && resourceType == that.resourceType && Objects.equals(assignment, that.assignment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceType, quantity, assignment);
    }
}
