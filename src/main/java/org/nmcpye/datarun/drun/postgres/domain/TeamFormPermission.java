package org.nmcpye.datarun.drun.postgres.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity
@Table(name = "team_form_permission", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"team_id", "form"})
})
//@TypeDefs(name = "jsonb", typeClass = JsonBinaryType.class)
//@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class TeamFormPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @NotNull
    @Size(max = 11)
    @Column(name = "form", nullable = false, length = 11)
    private String form;

    //    @ElementCollection(fetch = FetchType.EAGER)
//    @CollectionTable(name = "team_form_permission_permissions", joinColumns = @JoinColumn(name = "team_form_permission_id"))
//    @Enumerated(EnumType.STRING)
//    @Column(name = "permission")
//    @Type(value = JsonBinaryType.class)
//    @Type(value = ListArrayType.class)
//    @Type(SqlTypes.NAMED_ENUM)
//    @Type(
//        value = ListArrayType.class,
//        parameters = {
//            @org.hibernate.annotations.Parameter(
//                name = ListArrayType.SQL_ARRAY_TYPE,
//                value = "permission"
//            )
//        }
//    )
//    @Column(
//        name = "permissions",
//        columnDefinition = "permissions[]"
//    )
//    @Column(name = "permissions", columnDefinition = "jsonb")
//    @Enumerated(EnumType.STRING)
    @Type(JsonType.class)
    @Column(name = "permissions", columnDefinition = "jsonb")
    private Set<FormPermission> permissions = new HashSet<>();

    // Constructors, getters, and setters

    public TeamFormPermission() {
    }

    public TeamFormPermission(Team team, String form, Set<FormPermission> permissions) {
        this.team = team;
        this.form = form;
        this.permissions = permissions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public Set<FormPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<FormPermission> permissions) {
        this.permissions = permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TeamFormPermission that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(team, that.team) && Objects.equals(form, that.form);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, team, form);
    }
}
