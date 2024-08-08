package org.nmcpye.datarun.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A OrgUnit.
 */
@Entity
@Table(name = "org_unit")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrgUnit extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, unique = true)
    private String uid;

    @Column(name = "code", unique = true)
    private String code;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Column(name = "ou_path", nullable = false)
    private String ouPath;

    @Column(name = "parent")
    private String parent;

    @Column(name = "parent_code")
    private String parentCode;

    @ManyToOne(optional = false)
    @NotNull
    private OuLevel level;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public OrgUnit id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public OrgUnit uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public OrgUnit code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public OrgUnit name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOuPath() {
        return this.ouPath;
    }

    public OrgUnit ouPath(String ouPath) {
        this.setOuPath(ouPath);
        return this;
    }

    public void setOuPath(String ouPath) {
        this.ouPath = ouPath;
    }

    public String getParent() {
        return this.parent;
    }

    public OrgUnit parent(String parent) {
        this.setParent(parent);
        return this;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getParentCode() {
        return this.parentCode;
    }

    public OrgUnit parentCode(String parentCode) {
        this.setParentCode(parentCode);
        return this;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public OuLevel getLevel() {
        return this.level;
    }

    public void setLevel(OuLevel ouLevel) {
        this.level = ouLevel;
    }

    public OrgUnit level(OuLevel ouLevel) {
        this.setLevel(ouLevel);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrgUnit)) {
            return false;
        }
        return getId() != null && getId().equals(((OrgUnit) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrgUnit{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", ouPath='" + getOuPath() + "'" +
            ", parent='" + getParent() + "'" +
            ", parentCode='" + getParentCode() + "'" +
            "}";
    }
}
