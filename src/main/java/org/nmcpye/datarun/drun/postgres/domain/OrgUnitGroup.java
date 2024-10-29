package org.nmcpye.datarun.drun.postgres.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.drun.postgres.common.BaseIdentifiableObject;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A OrgUnitGroup.
 */
@Entity
@Table(name = "org_unit_group")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrgUnitGroup extends BaseIdentifiableObject<Long> implements Serializable {

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

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "color")
    private String color;

    @Column(name = "inactive")
    private Boolean inactive = false;


    @ManyToMany
    @JoinTable(
        name = "org_unit_group_members",
        joinColumns = @JoinColumn(name = "org_unit_group_id"),
        inverseJoinColumns = @JoinColumn(name = "member_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<OrgUnit> members = new HashSet<>();

    @ManyToMany(mappedBy = "orgUnitGroups")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonSerialize(contentAs = IdentifiableObject.class)
    private Set<OrgUnitGroupSet> groupSets = new HashSet<>();

    public boolean addOrgUnit(OrgUnit orgUnit) {
        members.add(orgUnit);
        return orgUnit.getGroups().add(this);
    }

    public void addOrgUnits(Set<OrgUnit> orgUnits) {
        orgUnits.forEach(this::addOrgUnit);
    }

    public boolean removeOrgUnit(OrgUnit orgUnit) {
        members.remove(orgUnit);
        return orgUnit.getGroups().remove(this);
    }

    public void removeOrgUnits(Set<OrgUnit> orgUnits) {
        orgUnits.forEach(this::removeOrgUnit);
    }

    public void removeAllOrgUnits() {
        for (OrgUnit orgUnit : members) {
            orgUnit.getGroups().remove(this);
        }

        members.clear();
    }

    public void updateOrgUnits(Set<OrgUnit> updates) {
        for (OrgUnit unit : new HashSet<>(members)) {
            if (!updates.contains(unit)) {
                removeOrgUnit(unit);
            }
        }

        for (OrgUnit unit : updates) {
            addOrgUnit(unit);
        }
    }

    public Long getId() {
        return this.id;
    }

    public OrgUnitGroup id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public OrgUnitGroup uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public OrgUnitGroup code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public OrgUnitGroup name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return this.symbol;
    }

    public OrgUnitGroup symbol(String symbol) {
        this.setSymbol(symbol);
        return this;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @JsonProperty
    public String getColor() {
        return this.color;
    }

    public OrgUnitGroup color(String color) {
        this.setColor(color);
        return this;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @JsonProperty
    public Boolean getInactive() {
        return this.inactive;
    }

    public OrgUnitGroup inactive(Boolean inactive) {
        this.setInactive(inactive);
        return this;
    }

    public void setInactive(Boolean inactive) {
        this.inactive = inactive;
    }

    @JsonProperty("orgUnits")
    @JsonSerialize(contentAs = BaseIdentifiableObject.class)
    public Set<OrgUnit> getMembers() {
        return this.members;
    }

    public void setMembers(Set<OrgUnit> orgUnits) {
        this.members = orgUnits;
    }

    public OrgUnitGroup members(Set<OrgUnit> orgUnits) {
        this.setMembers(orgUnits);
        return this;
    }

    public OrgUnitGroup addMember(OrgUnit orgUnit) {
        this.members.add(orgUnit);
        orgUnit.getGroups().add(this);
        return this;
    }

    public OrgUnitGroup removeMember(OrgUnit orgUnit) {
        this.members.remove(orgUnit);
        orgUnit.getGroups().remove(this);
        return this;
    }

    public Set<OrgUnitGroupSet> getGroupSets() {
        return this.groupSets;
    }

    public void setGroupSets(Set<OrgUnitGroupSet> orgUnitGroupSets) {
//        if (this.groupSets != null) {
//            this.groupSets.forEach(i -> i.removeOrgUnitGroup(this));
//        }
//        if (orgUnitGroupSets != null) {
//            orgUnitGroupSets.forEach(i -> i.addOrgUnitGroup(this));
//        }
        this.groupSets = orgUnitGroupSets;
    }

    public OrgUnitGroup groupSets(Set<OrgUnitGroupSet> orgUnitGroupSets) {
        this.setGroupSets(orgUnitGroupSets);
        return this;
    }

    public OrgUnitGroup addGroupSet(OrgUnitGroupSet orgUnitGroupSet) {
        this.groupSets.add(orgUnitGroupSet);
        orgUnitGroupSet.getOrgUnitGroups().add(this);
        return this;
    }

    public OrgUnitGroup removeGroupSet(OrgUnitGroupSet orgUnitGroupSet) {
        this.groupSets.remove(orgUnitGroupSet);
        orgUnitGroupSet.getOrgUnitGroups().remove(this);
        return this;
    }
}
