package org.nmcpye.datarun.service.custom.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.domain.enumeration.PublicLocationType;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link org.nmcpye.datarun.domain.VillageLocation} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class VillageLocationDTO implements Serializable {

    @Size(max = 11)
    private String uid;

    @NotNull
    private String code;

    private String name;

    private Integer mappingStatus;

    private Integer districtCode;

    private String villageUid;

    private String subdistrictName;

    private String villageName;

    private String subvillageName;

    private Integer urbanRuralId;

    private String urbanRural;

    private String settlement;

    private Double pop2004;

    private Double pop2022;

    private Double longitude;

    private Double latitude;

    @NotNull
    private String ppcCodeGis;

    private PublicLocationType level;

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

    public Integer getMappingStatus() {
        return mappingStatus;
    }

    public void setMappingStatus(Integer mappingStatus) {
        this.mappingStatus = mappingStatus;
    }

    public Integer getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(Integer districtCode) {
        this.districtCode = districtCode;
    }

    public String getVillageUid() {
        return villageUid;
    }

    public void setVillageUid(String villageUid) {
        this.villageUid = villageUid;
    }

    public String getSubdistrictName() {
        return subdistrictName;
    }

    public void setSubdistrictName(String subdistrictName) {
        this.subdistrictName = subdistrictName;
    }

    public String getVillageName() {
        return villageName;
    }

    public void setVillageName(String villageName) {
        this.villageName = villageName;
    }

    public String getSubvillageName() {
        return subvillageName;
    }

    public void setSubvillageName(String subvillageName) {
        this.subvillageName = subvillageName;
    }

    public Integer getUrbanRuralId() {
        return urbanRuralId;
    }

    public void setUrbanRuralId(Integer urbanRuralId) {
        this.urbanRuralId = urbanRuralId;
    }

    public String getUrbanRural() {
        return urbanRural;
    }

    public void setUrbanRural(String urbanRural) {
        this.urbanRural = urbanRural;
    }

    public String getSettlement() {
        return settlement;
    }

    public void setSettlement(String settlement) {
        this.settlement = settlement;
    }

    public Double getPop2004() {
        return pop2004;
    }

    public void setPop2004(Double pop2004) {
        this.pop2004 = pop2004;
    }

    public Double getPop2022() {
        return pop2022;
    }

    public void setPop2022(Double pop2022) {
        this.pop2022 = pop2022;
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

    public String getPpcCodeGis() {
        return ppcCodeGis;
    }

    public void setPpcCodeGis(String ppcCodeGis) {
        this.ppcCodeGis = ppcCodeGis;
    }

    public PublicLocationType getLevel() {
        return level;
    }

    public void setLevel(PublicLocationType level) {
        this.level = level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VillageLocationDTO)) {
            return false;
        }

        VillageLocationDTO villageLocationDTO = (VillageLocationDTO) o;
        if (this.uid == null) {
            return false;
        }
        return Objects.equals(this.uid, villageLocationDTO.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uid);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "VillageLocationDTO{" +
            "uid=" + getUid() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", mappingStatus=" + getMappingStatus() +
            ", districtCode=" + getDistrictCode() +
            ", villageUid='" + getVillageUid() + "'" +
            ", subdistrictName='" + getSubdistrictName() + "'" +
            ", villageName='" + getVillageName() + "'" +
            ", subvillageName='" + getSubvillageName() + "'" +
            ", urbanRuralId=" + getUrbanRuralId() +
            ", urbanRural='" + getUrbanRural() + "'" +
            ", settlement='" + getSettlement() + "'" +
            ", pop2004=" + getPop2004() +
            ", pop2022=" + getPop2022() +
            ", longitude=" + getLongitude() +
            ", latitude=" + getLatitude() +
            ", ppcCodeGis='" + getPpcCodeGis() + "'" +
            ", level='" + getLevel() + "'" +
            "}";
    }
}
