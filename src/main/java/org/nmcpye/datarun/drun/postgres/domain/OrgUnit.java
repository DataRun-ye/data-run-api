package org.nmcpye.datarun.drun.postgres.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.drun.postgres.common.BaseIdentifiableObject;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableObjectUtils;

import java.io.Serializable;
import java.util.*;

/**
 * A OrgUnit.
 */
@Entity
@Table(name = "org_unit")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrgUnit extends BaseIdentifiableObject<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String PATH_SEP = ",";

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
    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "parent_uid")
    private String parentUid;

    @Column(name = "parent_code")
    private String parentCode;

//    @ManyToOne(optional = false)
//    @NotNull
//    private OuLevel level;

    @JsonIgnore
//    @JsonProperty(value = "level", access = JsonProperty.Access.READ_ONLY)
    @Column(name = "level")
    private Integer hierarchyLevel;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "orgUnit")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"orgUnit"}, allowSetters = true)
    private Set<Assignment> assignments = new HashSet<>();

    @ManyToOne//(fetch = FetchType.LAZY)
    @JsonProperty
    @JsonIgnoreProperties(value = {"parent", "assignments", "ancestors", "level", "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy" }, allowSetters = true)
//    @JsonSerialize(as = IdentifiableObject.class)
    private OrgUnit parent;

//    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
//    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
//    @JsonSerialize(contentAs = IdentifiableObject.class)
//    private Set<OrgUnit> children = new HashSet<>();

    public Set<Assignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(Set<Assignment> assignments) {
        this.assignments = assignments;
    }

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

    public String getPath() {
        return this.path;
    }

    public OrgUnit path(String ouPath) {
        this.setPath(ouPath);
        return this;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getParentUid() {
        return this.parentUid;
    }

    public OrgUnit parent(String parent) {
        this.setParentUid(parent);
        return this;
    }

    public void setParentUid(String parentUid) {
        this.parentUid = parentUid;
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

//    public OuLevel getLevel() {
//        return this.level;
//    }
//
//    public void setLevel(OuLevel ouLevel) {
//        this.level = ouLevel;
//    }
//
//    public OrgUnit level(OuLevel ouLevel) {
//        this.setLevel(ouLevel);
//        return this;
//    }

    public OrgUnit getParent() {
        return parent;
    }

    public void setParent(OrgUnit parent) {
        this.parent = parent;
    }

    /**
     * Returns the list of ancestor organisation unit UIDs up to any of the given
     * roots for this organisation unit. Does not include itself. The list is
     * ordered by root first.
     *
     * @param rootUids the root organisation units, if null using real roots.
     */
    public List<String> getAncestorUids(Set<String> rootUids) {
        if (path == null || path.isEmpty()) {
            return Lists.newArrayList();
        }

        String[] ancestors = path.substring(1).split(PATH_SEP); // Skip first delimiter, root unit first
        int lastIndex = ancestors.length - 2; // Skip this unit
        List<String> uids = Lists.newArrayList();

        for (int i = lastIndex; i >= 0; i--) {
            String uid = ancestors[i];
            uids.add(0, uid);

            if (rootUids != null && rootUids.contains(uid)) {
                break;
            }
        }

        return uids;
    }

    /**
     * Returns a string representing the graph of ancestors. The string is delimited
     * by "/". The ancestors are ordered by root first and represented by UIDs.
     *
     * @param roots the root organisation units, if null using real roots.
     */
    public String getParentGraph(Collection<OrgUnit> roots) {
        Set<String> rootUids = roots != null ? Sets.newHashSet(IdentifiableObjectUtils.getUids(roots)) : null;
        List<String> ancestors = getAncestorUids(rootUids);
        return StringUtils.join(ancestors, PATH_SEP);
    }

    /**
     * Returns a string representing the graph of ancestors. The string is delimited
     * by ",". The ancestors are ordered by root first and represented by names.
     *
     * @param roots       the root organisation units, if null using real roots.
     * @param includeThis whether to include this organisation unit in the graph.
     */
    public String getParentNameGraph(Collection<OrgUnit> roots, boolean includeThis) {
        StringBuilder builder = new StringBuilder();

        List<OrgUnit> ancestors = getAncestors(roots);

        for (OrgUnit unit : ancestors) {
            builder.append(PATH_SEP).append(unit.getName());
        }

        if (includeThis) {
            builder.append(PATH_SEP).append(name);
        }

        return builder.toString();
    }

    /**
     * Returns a mapping between the uid and the uid parent graph of the given
     * organisation units.
     */
    public static Map<String, String> getParentGraphMap(List<OrgUnit> organisationUnits, Collection<OrgUnit> roots) {
        Map<String, String> map = new HashMap<>();

        if (organisationUnits != null) {
            for (OrgUnit unit : organisationUnits) {
                map.put(unit.getUid(), unit.getParentGraph(roots));
            }
        }

        return map;
    }

//    @JsonProperty(value = "level", access = JsonProperty.Access.READ_ONLY)
    public Integer getLevel() {
        return StringUtils.countMatches(path, PATH_SEP);
    }

    // for Hibernate
    public void setLevel(Integer ouLevel) {
        //this.level = ouLevel;
    }

    /**
     * Used by persistence layer. Purpose is to have a column for use in database
     * queries. For application use see {@link OrgUnit#getLevel()} which
     * has better performance.
     */
    public Integer getHierarchyLevel() {
        Set<String> uids = Sets.newHashSet(uid);

        OrgUnit current = this;

        while ((current = current.getParent()) != null) {
            boolean add = uids.add(current.getUid());

            if (!add) {
                break; // Protect against cyclic org unit graphs
            }
        }

        hierarchyLevel = uids.size();

        return hierarchyLevel;
    }

    public OrgUnit hierarchyLevel(Integer hierarchyLevel) {
        this.setHierarchyLevel(hierarchyLevel);
        return this;
    }

    public void setHierarchyLevel(Integer hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }

    /**
     * Returns the list of ancestor organisation units for this organisation unit.
     * Does not include itself. The list is ordered by root first.
     *
     * @throws IllegalStateException if circular parent relationships is detected.
     */
//    @JsonProperty("ancestors")
    @JsonSerialize(contentAs = IdentifiableObject.class)
    public List<OrgUnit> getAncestors() {
        List<OrgUnit> units = new ArrayList<>();
        Set<OrgUnit> visitedUnits = new HashSet<>();

        OrgUnit unit = parent;

        while (unit != null) {
            if (!visitedUnits.add(unit)) {
                throw new IllegalStateException(
                    "Organisation unit '" + this.toString() + "' has circular parent relationships: '" + unit + "'"
                );
            }

            units.add(unit);
            unit = unit.getParent();
        }

        Collections.reverse(units);
        return units;
    }


    /**
     * Returns the list of ancestor organisation units up to any of the given roots
     * for this organisation unit. Does not include itself. The list is ordered by
     * root first.
     *
     * @param roots the root organisation units, if null using real roots.
     */
    public List<OrgUnit> getAncestors(Collection<OrgUnit> roots) {
        List<OrgUnit> units = new ArrayList<>();
        OrgUnit unit = parent;

        while (unit != null) {
            units.add(unit);

            if (roots != null && roots.contains(unit)) {
                break;
            }

            unit = unit.getParent();
        }

        Collections.reverse(units);
        return units;
    }

//    @JsonProperty
//    public Set<OrgUnit> getChildren() {
//        return this.children;
//    }
//
//    public void setChildren(Set<OrgUnit> organisationUnits) {
////        if (this.children != null) {
////            this.children.forEach(i -> i.setParent(null));
////        }
////        if (organisationUnits != null) {
////            organisationUnits.forEach(i -> i.setParent(this));
////        }
//        this.children = organisationUnits;
//    }
//
//    public OrgUnit children(Set<OrgUnit> organisationUnits) {
//        this.setChildren(organisationUnits);
//        return this;
//    }
//
//    public OrgUnit addChildren(OrgUnit organisationUnit) {
//        this.children.add(organisationUnit);
//        organisationUnit.setParent(this);
//        return this;
//    }
//
//    public OrgUnit removeChildren(OrgUnit organisationUnit) {
//        this.children.remove(organisationUnit);
//        organisationUnit.setParent(null);
//        return this;
//    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrgUnit{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", ouPath='" + getPath() + "'" +
            ", parent='" + getParentUid() + "'" +
            ", parentCode='" + getParentCode() + "'" +
            "}";
    }
}
