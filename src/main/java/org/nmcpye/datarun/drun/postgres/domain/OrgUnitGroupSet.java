package org.nmcpye.datarun.drun.postgres.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.drun.postgres.common.BaseIdentifiableObject;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A OrgUnitGroupSet.
 */
@Entity
@Table(name = "org_unit_groupset")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrgUnitGroupSet
    extends BaseIdentifiableObject<Long> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, unique = true, nullable = false)
    private String uid;

    @Column(name = "code", unique = true)
    private String code;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @ManyToMany
    @JoinTable(
        name = "org_unit_groupset_org_unit_group",
        joinColumns = @JoinColumn(name = "org_unit_groupset_id"),
        inverseJoinColumns = @JoinColumn(name = "org_unit_group_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "groupSets", "members" }, allowSetters = true)
//    @JsonSerialize(contentAs = IdentifiableObject.class)
    private Set<OrgUnitGroup> orgUnitGroups = new HashSet<>();

    public OrgUnitGroupSet() {
        this.setAutoFields();
    }

    public boolean hasOrgUnitGroups() {
        return orgUnitGroups != null && orgUnitGroups.size() > 0;
    }

    public boolean isMemberOfOrgUnitGroups(OrgUnit organisationUnit) {
        for (OrgUnitGroup group : orgUnitGroups) {
            if (group.getMembers().contains(organisationUnit)) {
                return true;
            }
        }

        return false;
    }

    public Long getId() {
        return this.id;
    }

    public OrgUnitGroupSet id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public OrgUnitGroupSet uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public OrgUnitGroupSet code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public OrgUnitGroupSet name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<OrgUnitGroup> getOrgUnitGroups() {
        return this.orgUnitGroups;
    }

    public void setOrgUnitGroups(Set<OrgUnitGroup> orgUnitGroups) {
        this.orgUnitGroups = orgUnitGroups;
    }

    public OrgUnitGroupSet orgUnitGroups(Set<OrgUnitGroup> orgUnitGroups) {
        this.setOrgUnitGroups(orgUnitGroups);
        return this;
    }

    public OrgUnitGroupSet addOrgUnitGroup(OrgUnitGroup orgUnitGroup) {
        this.orgUnitGroups.add(orgUnitGroup);
        orgUnitGroup.getGroupSets().add(this);
        return this;
    }

    public OrgUnitGroupSet removeOrgUnitGroup(OrgUnitGroup orgUnitGroup) {
        this.orgUnitGroups.remove(orgUnitGroup);
        orgUnitGroup.getGroupSets().remove(this);
        return this;
    }
}
