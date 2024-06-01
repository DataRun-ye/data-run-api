package org.nmcpye.datarun.service.custom.dto;

import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A DTO for the {@link org.nmcpye.datarun.domain.Project} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ProjectDTO implements Serializable {
    // This will be the UID
    @Size(max = 11)
    private String uid;

    private String code;

    private String name;

    private Boolean displayed;

    private List<ActivityDTO> activities = new ArrayList<>();

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getDisplayed() {
        return displayed;
    }

    public void setDisplayed(Boolean displayed) {
        this.displayed = displayed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProjectDTO)) {
            return false;
        }

        ProjectDTO projectDTO = (ProjectDTO) o;
        if (this.uid == null) {
            return false;
        }
        return Objects.equals(this.uid, projectDTO.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uid);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ProjectDTO{" +
            "uid=" + getUid() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", displayed='" + getDisplayed() + "'" +
            "}";
    }

    public List<ActivityDTO> getActivities() {
        return activities;
    }

    public void setActivities(List<ActivityDTO> activities) {
        this.activities = activities;
    }
}
