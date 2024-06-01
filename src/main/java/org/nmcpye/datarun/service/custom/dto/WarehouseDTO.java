package org.nmcpye.datarun.service.custom.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link org.nmcpye.datarun.domain.Warehouse} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class WarehouseDTO implements Serializable {

    @Size(max = 11)
    private String uid;

    @NotNull
    private String code;

    private String name;

    private String description;

    private String gpsCoordinate;

    private String supervisor;

    private String supervisorMobile;

    private ActivityDTO activity;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGpsCoordinate() {
        return gpsCoordinate;
    }

    public void setGpsCoordinate(String gpsCoordinate) {
        this.gpsCoordinate = gpsCoordinate;
    }

    public String getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(String supervisor) {
        this.supervisor = supervisor;
    }

    public String getSupervisorMobile() {
        return supervisorMobile;
    }

    public void setSupervisorMobile(String supervisorMobile) {
        this.supervisorMobile = supervisorMobile;
    }

    public ActivityDTO getActivity() {
        return activity;
    }

    public void setActivity(ActivityDTO activity) {
        this.activity = activity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WarehouseDTO)) {
            return false;
        }

        WarehouseDTO warehouseDTO = (WarehouseDTO) o;
        if (this.uid == null) {
            return false;
        }
        return Objects.equals(this.uid, warehouseDTO.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uid);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "WarehouseDTO{" +
            "uid=" + getUid() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", gpsCoordinate='" + getGpsCoordinate() + "'" +
            ", supervisor='" + getSupervisor() + "'" +
            ", supervisorMobile='" + getSupervisorMobile() + "'" +
            ", activity=" + getActivity() +
            "}";
    }
}
