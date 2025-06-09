package org.nmcpye.datarun.jpa.flowrun;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.common.IdentifiableObjectUtils;
import org.nmcpye.datarun.common.enumeration.FlowRunStatus;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.common.JpaSoftDeleteObject;
import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.team.Team;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An FlowRun, can be looked at as an FlowType instance.
 * <pre>
 *     {@code
 *  FlowRun {
 *       id: "A123",
 *       typeId: "malariaCampaign",
 *       assignedTo: "...",
 *       status: "IN_PROGRESS",
 *       stageSubmissions: {
 *        registration:  { submissionId: "S1", status: "COMPLETED" },
 *         caseVisit:     [ { submissionId: "S2" }, { submissionId: "S3" } ],  // repeatable
 *         treatmentFollow: null
 *      }
 *    }
 *  }
 * </pre>
 */
@Entity
@Table(name = "flow_run", uniqueConstraints = {
    @UniqueConstraint(name = "uc_flow_run_uid", columnNames = "uid"),
    @UniqueConstraint(name = "uc_flow_run_activity_id", columnNames = {"activity_id", "team_id", "org_unit_id"})
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FlowRun extends JpaSoftDeleteObject {
    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false)
    private String uid;

    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flow_type_id")
    private FlowType flowType;

    /**
     * Json map of scopeKey → chosenValue (string or UUID text)
     * team: {value: "teamUID1", type: "Team"}
     */
    @Type(JsonType.class)
    @Column(name = "scopes", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> scopes;

    @Enumerated(EnumType.STRING)
    private FlowRunStatus status = FlowRunStatus.PLANNED;

    /**
     * A map of stageId → list of stageInstance/submissions Uids (as JSON),
     * or = null if SINGLE/SINGLE stage.
     * not strictly required, just to Speed Up UI & Workflow Checks, Model the
     * “Flow Run State Machine” so it can store additional per‐stage metadata,
     * and to avoid Joins Under Heavy Load
     */
    @JsonProperty
    @Type(JsonType.class)
    @Column(name = "steps_states", columnDefinition = "jsonb")
    private Map<String, List<String>> stepStates;

    // --------------------------------------
    // TODO remove and replace with scope values
    // --------------------------------------

    private static final String PATH_SEP = ",";

    @ManyToOne
    @JsonIgnoreProperties(value = {"project", "translations", "flowRuns"}, allowSetters = true)
    private Activity activity;

    @ManyToOne
    @JsonIgnoreProperties(value = {"managedTeams", "managedByTeams", "users", "flowRuns",
        "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy", "activity", "teamFormAccesses", "formPermissions"}, allowSetters = true)
    private Team team;

    @ManyToOne
    @JsonIgnoreProperties(value = {"parent", "children", "orgUnitGroups", "flowRuns",
        "hierarchyLevel", "ancestors", "translations"}, allowSetters = true)
    private OrgUnit orgUnit;

    @Column(name = "start_day")
    private Integer startDay;

    @Column(name = "start_date")
    private Instant startDate;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"activity", "team", "orgUnit", "parent", "children", "ancestors", "level", "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy"}, allowSetters = true)
    private Set<FlowRun> children = new HashSet<>();

    @Column(name = "path")
    private String path;

    @JsonIgnore
    @Column(name = "level")
    private Integer hierarchyLevel;

    @ManyToOne
    @JsonProperty
    @JsonIgnoreProperties(value = {"activity", "team", "orgUnit", "parent", "children", "ancestors", "level", "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy"}, allowSetters = true)
    private FlowRun parent;

    @Type(JsonType.class)
    @Column(name = "forms", columnDefinition = "jsonb")
    private Set<String> forms = new HashSet<>();

    /**
     * If you plan to pre-link an entity (e.g. a Household),
     * otherwise = null for “new” entities.
     */
    @Column(name = "entity_instance_uid")
    private String entityInstanceUid;

    @Override
    public String getCode() {
        return orgUnit != null ? orgUnit.getCode() : null;
    }

    @Override
    public String getName() {
        return orgUnit != null ? orgUnit.getCode() + ":" + orgUnit.getName() : null;
    }

    @JsonProperty(value = "flowRunStatus")
    public FlowRunStatus getStatus() {
        return status;
    }

//    @Deprecated(since = "v7")
//    @JsonProperty
//    public Map<String, Object> getAllocatedResources() {
//        return allocatedResources;
//    }

    @Deprecated(since = "v7")
    public Activity getActivity() {
        return activity;
    }

    /**
     * Returns the list of ancestor flowRun UIDs up to any of the given
     * roots for this flowRun. Does not include itself. The list is
     * ordered by root first.
     *
     * @param rootUids the root activities, if null using real roots.
     */
    public List<String> getAncestorUids(Set<String> rootUids) {
        if (path == null || path.isEmpty()) {
            return Lists.newArrayList();
        }

        String[] ancestors = path.substring(1).split(PATH_SEP); // Skip first delimiter, root flowRun first
        int lastIndex = ancestors.length - 2; // Skip this flowRun
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
     * @param roots the root activities, if null using real roots.
     */
    public String getParentGraph(Collection<FlowRun> roots) {
        Set<String> rootUids = roots != null ? Sets.newHashSet(String.valueOf(IdentifiableObjectUtils.getUids(roots))) : null;
        List<String> ancestors = getAncestorUids(rootUids);
        return StringUtils.join(ancestors, PATH_SEP);
    }

    /**
     * Returns a mapping between the uid and the uid parent graph of the given
     * activities.
     */
    public static Map<String, String> getParentGraphMap(List<FlowRun> activities, Collection<FlowRun> roots) {
        Map<String, String> map = new HashMap<>();

        if (activities != null) {
            for (FlowRun flowRun : activities) {
                map.put(flowRun.getUid(), flowRun.getParentGraph(roots));
            }
        }

        return map;
    }

    @JsonProperty(value = "level", access = JsonProperty.Access.READ_ONLY)
    public Integer getLevel() {
        return StringUtils.countMatches(path, PATH_SEP);
    }

    // for Hibernate
    public void setLevel(Integer ouLevel) {
        //this.level = ouLevel;
    }

    /**
     * Used by persistence layer. Purpose is to have a column for use in database
     * queries. For application use see {@link FlowRun#getLevel()} which
     * has better performance.
     */
    public Integer getHierarchyLevel() {
        Set<String> uids = Sets.newHashSet(getUid());

        FlowRun current = this;

        while ((current = current.getParent()) != null) {
            boolean add = uids.add(current.getUid());

            if (!add) {
                break; // Protect against cyclic org flowRun graphs
            }
        }

        hierarchyLevel = uids.size();

        return hierarchyLevel;
    }

    public FlowRun hierarchyLevel(Integer hierarchyLevel) {
        this.setHierarchyLevel(hierarchyLevel);
        return this;
    }

    /**
     * Returns the list of ancestor activities for this flowRun.
     * Does not include itself. The list is ordered by root first.
     *
     * @throws IllegalStateException if circular parent relationships is detected.
     */
    @JsonIgnore
//    @JsonProperty("ancestors")
//    @JsonSerialize(contentAs = Identifiable.class)
    public List<FlowRun> getAncestors() {
        List<FlowRun> activities = new ArrayList<>();
        Set<FlowRun> visitedActivities = new HashSet<>();

        FlowRun flowRun = parent;

        while (flowRun != null) {
            if (!visitedActivities.add(flowRun)) {
                throw new IllegalStateException(
                    "Assignment '" + this.toString() + "' has circular parent relationships: '" + flowRun + "'"
                );
            }

            activities.add(flowRun);
            flowRun = flowRun.getParent();
        }

        Collections.reverse(activities);
        return activities;
    }

    /**
     * Returns the list of ancestors up to any of the given roots
     * for this flowRun. Does not include itself. The list is ordered by
     * root first.
     *
     * @param roots the root activities, if null using real roots.
     */
    public List<FlowRun> getAncestors(Collection<FlowRun> roots) {
        List<FlowRun> flowRuns = new ArrayList<>();
        FlowRun flowRun = parent;

        while (flowRun != null) {
            flowRuns.add(flowRun);

            if (roots != null && roots.contains(flowRun)) {
                break;
            }

            flowRun = flowRun.getParent();
        }

        Collections.reverse(flowRuns);
        return flowRuns;
    }

    //    public String getPath() {
//        return path;
//    }
    public String getPath() {
        List<String> pathList = new ArrayList<>();
        Set<String> visitedSet = new HashSet<>();
        FlowRun flowRun = parent;

        pathList.add(getUid());

        while (flowRun != null) {
            if (!visitedSet.contains(flowRun.getUid())) {
                pathList.add(flowRun.getUid());
                visitedSet.add(flowRun.getUid());
                flowRun = flowRun.getParent();
            } else {
                flowRun = null; // Protect against cyclic org unit graphs
            }
        }

        Collections.reverse(pathList);

        this.path = PATH_SEP + StringUtils.join(pathList, PATH_SEP);

        return this.path;
    }

    public boolean isDescendant(FlowRun ancestor) {
        if (ancestor == null) {
            return false;
        }

        FlowRun unit = this;

        while (unit != null) {
            if (ancestor.equals(unit)) {
                return true;
            }

            unit = unit.getParent();
        }

        return false;
    }

    @JsonIgnore
    public boolean isDescendant(Set<FlowRun> ancestors) {
        if (ancestors == null || ancestors.isEmpty()) {
            return false;
        }
        Set<String> ancestorsUid = ancestors.stream().map(FlowRun::getUid).collect(Collectors.toSet());

        FlowRun unit = this;

        while (unit != null) {
            if (ancestorsUid.contains(unit.getUid())) {
                return true;
            }

            unit = unit.getParent();
        }

        return false;
    }


    @Override
    public String toString() {
        return "Assignment{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", createdBy='" + getCreatedBy() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedBy='" + getLastModifiedBy() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            "}";
    }
}
