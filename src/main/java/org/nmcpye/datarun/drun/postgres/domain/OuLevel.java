package org.nmcpye.datarun.drun.postgres.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;

/**
 * A OuLevel.
 */
@Entity
@Table(name = "ou_level", indexes = {
    @Index(name = "idx_oulevel_uid_unq", columnList = "uid", unique = true)
}, uniqueConstraints = {
    @UniqueConstraint(name = "uc_ou_level_name", columnNames = "name"),
    @UniqueConstraint(name = "uc_ou_level_code", columnNames = "code")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OuLevel extends JpaBaseIdentifiableObject {
    @NotNull
    @Column(name = "level", nullable = false)
    private Integer level;

    @Override
    public String toString() {
        return "OuLevel{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", name='" + getName() + "'" +
            ", level=" + getLevel() +
            "}";
    }
}
