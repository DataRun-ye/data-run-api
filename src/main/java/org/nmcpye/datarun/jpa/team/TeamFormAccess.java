package org.nmcpye.datarun.jpa.team;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hibernate.proxy.HibernateProxy;
import org.nmcpye.datarun.common.enumeration.FormPermission;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * A TeamFormPermission.
 */
@Entity
@Table(name = "team_form_access", indexes = {
    @Index(name = "idx_team_form_access_form_uid", columnList = "form_uid"),
}, uniqueConstraints = {
    @UniqueConstraint(name = "uc_team_form_access_form_uid", columnNames = {"team_id", "form_uid"})
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TeamFormAccess implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    protected Long id;

    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = {"managedTeams", "managedByTeams", "users", "assignments",
        "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy", "activity", "teamFormAccesses", "formPermissions"}, allowSetters = true)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @NotNull
    @Column(name = "form_uid", nullable = false)
    private String formUid;

    @Type(JsonType.class)
    @Column(name = "permissions", columnDefinition = "jsonb")
    private Set<FormPermission> permissions;

    @JsonIgnore
    public Long getId() {
        return id;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o)
            .getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this)
            .getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        TeamFormAccess that = (TeamFormAccess) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this)
            .getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
