package org.nmcpye.datarun.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;

import java.util.HashSet;
import java.util.Set;

/**
 * A Project.
 */
@Entity
@Table(name = "project", indexes = {
    @Index(name = "idx_project_uid_unq", columnList = "uid", unique = true)
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(value = {"new"})
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Project extends JpaBaseIdentifiableObject {

    @Column(name = "disabled")
    private Boolean disabled;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "project")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"project"}, allowSetters = true)
    private Set<Activity> activities = new HashSet<>();
}
