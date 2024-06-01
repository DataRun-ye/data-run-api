package org.nmcpye.datarun.service.custom.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.service.dto.UserDTO;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link org.nmcpye.datarun.domain.Team} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TeamDTO implements Serializable {

    @Size(max = 11)
    private String uid;

    @NotNull
    private String code;

    private String name;

    private String description;

    private String mobile;

    private Integer workers;

    private String mobility;

    private ActivityDTO activity;

    private ReviewTeamDTO operationRoom;

    private WarehouseDTO warehouse;

    private UserDTO userInfo;

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

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Integer getWorkers() {
        return workers;
    }

    public void setWorkers(Integer workers) {
        this.workers = workers;
    }

    public String getMobility() {
        return mobility;
    }

    public void setMobility(String mobility) {
        this.mobility = mobility;
    }

    public ActivityDTO getActivity() {
        return activity;
    }

    public void setActivity(ActivityDTO activity) {
        this.activity = activity;
    }

    public ReviewTeamDTO getOperationRoom() {
        return operationRoom;
    }

    public void setOperationRoom(ReviewTeamDTO operationRoom) {
        this.operationRoom = operationRoom;
    }

    public WarehouseDTO getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(WarehouseDTO warehouse) {
        this.warehouse = warehouse;
    }

    public UserDTO getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserDTO userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TeamDTO)) {
            return false;
        }

        TeamDTO teamDTO = (TeamDTO) o;
        if (this.uid == null) {
            return false;
        }
        return Objects.equals(this.uid, teamDTO.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uid);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TeamDTO{" +
            "uid=" + getUid() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", mobile='" + getMobile() + "'" +
            ", workers=" + getWorkers() +
            ", mobility='" + getMobility() + "'" +
            ", activity=" + getActivity() +
            ", operationRoom=" + getOperationRoom() +
            ", warehouse=" + getWarehouse() +
            ", userInfo=" + getUserInfo() +
            "}";
    }
}
