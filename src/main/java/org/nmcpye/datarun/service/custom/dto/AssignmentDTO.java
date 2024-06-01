package org.nmcpye.datarun.service.custom.dto;

import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link org.nmcpye.datarun.domain.Assignment} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AssignmentDTO implements Serializable {
    @Size(max = 11)
    private String uid;

    private String code;

    private Integer phaseNo;

    private Integer districtCode;

    private String gov;

    private String district;

    private String subdistrict;

    private String village;

    private String subvillage;

    private String name;

    private Integer dayId;

    private Double population;

    private Integer itnsPlanned;

    private Integer targetType;

    private Double longitude;

    private Double latitude;

    private Instant startDate;

    private ActivityDTO activity;

    private VillageLocationDTO organisationUnit;

    private TeamDTO team;

    private WarehouseDTO warehouse;

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

    public Integer getPhaseNo() {
        return phaseNo;
    }

    public void setPhaseNo(Integer phaseNo) {
        this.phaseNo = phaseNo;
    }

    public Integer getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(Integer districtCode) {
        this.districtCode = districtCode;
    }

    public String getGov() {
        return gov;
    }

    public void setGov(String gov) {
        this.gov = gov;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getSubdistrict() {
        return subdistrict;
    }

    public void setSubdistrict(String subdistrict) {
        this.subdistrict = subdistrict;
    }

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village;
    }

    public String getSubvillage() {
        return subvillage;
    }

    public void setSubvillage(String subvillage) {
        this.subvillage = subvillage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDayId() {
        return dayId;
    }

    public void setDayId(Integer dayId) {
        this.dayId = dayId;
    }

    public Double getPopulation() {
        return population;
    }

    public void setPopulation(Double population) {
        this.population = population;
    }

    public Integer getItnsPlanned() {
        return itnsPlanned;
    }

    public void setItnsPlanned(Integer itnsPlanned) {
        this.itnsPlanned = itnsPlanned;
    }

    public Integer getTargetType() {
        return targetType;
    }

    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public ActivityDTO getActivity() {
        return activity;
    }

    public void setActivity(ActivityDTO activity) {
        this.activity = activity;
    }

    public VillageLocationDTO getOrganisationUnit() {
        return organisationUnit;
    }

    public void setOrganisationUnit(VillageLocationDTO organisationUnit) {
        this.organisationUnit = organisationUnit;
    }

    public TeamDTO getTeam() {
        return team;
    }

    public void setTeam(TeamDTO team) {
        this.team = team;
    }

    public WarehouseDTO getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(WarehouseDTO warehouse) {
        this.warehouse = warehouse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AssignmentDTO)) {
            return false;
        }

        AssignmentDTO assignmentDTO = (AssignmentDTO) o;
        if (this.uid == null) {
            return false;
        }
        return Objects.equals(this.uid, assignmentDTO.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uid);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AssignmentDTO{" +
            "uid=" + getUid() +
            ", code='" + getCode() + "'" +
            ", phaseNo=" + getPhaseNo() +
            ", districtCode=" + getDistrictCode() +
            ", gov='" + getGov() + "'" +
            ", district='" + getDistrict() + "'" +
            ", subdistrict='" + getSubdistrict() + "'" +
            ", village='" + getVillage() + "'" +
            ", subvillage='" + getSubvillage() + "'" +
            ", name='" + getName() + "'" +
            ", dayId=" + getDayId() +
            ", population=" + getPopulation() +
            ", itnsPlanned=" + getItnsPlanned() +
            ", targetType=" + getTargetType() +
            ", longitude=" + getLongitude() +
            ", latitude=" + getLatitude() +
            ", startDate='" + getStartDate() + "'" +
            ", activity=" + getActivity() +
            ", organisationUnit=" + getOrganisationUnit() +
            ", team=" + getTeam() +
            ", warehouse=" + getWarehouse() +
            "}";
    }
}
