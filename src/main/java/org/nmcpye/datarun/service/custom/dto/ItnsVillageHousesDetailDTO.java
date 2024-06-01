package org.nmcpye.datarun.service.custom.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link org.nmcpye.datarun.domain.ItnsVillageHousesDetail} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ItnsVillageHousesDetailDTO implements Serializable {

    @Size(max = 11)
    private String uid;

    private String code;

    private Long couponId;

    private String name;

    @Min(value = 0)
    private Integer male;

    @Min(value = 0)
    private Integer female;

    @Min(value = 0)
    private Integer pregnant;

    @Min(value = 0)
    private Integer population;

    @Min(value = 0)
    private Integer maleChild;

    @Min(value = 0)
    private Integer femaleChild;

    @Min(value = 0)
    private Integer displaced;

    @Min(value = 0)
    private Integer itns;

    @Size(max = 2000)
    private String comment;

    private String submissionUuid;

    private Boolean deleted;

    private String houseUuid;

    @NotNull
    private ItnsVillageDTO villageData;

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

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMale() {
        return male;
    }

    public void setMale(Integer male) {
        this.male = male;
    }

    public Integer getFemale() {
        return female;
    }

    public void setFemale(Integer female) {
        this.female = female;
    }

    public Integer getPregnant() {
        return pregnant;
    }

    public void setPregnant(Integer pregnant) {
        this.pregnant = pregnant;
    }

    public Integer getPopulation() {
        return population;
    }

    public void setPopulation(Integer population) {
        this.population = population;
    }

    public Integer getMaleChild() {
        return maleChild;
    }

    public void setMaleChild(Integer maleChild) {
        this.maleChild = maleChild;
    }

    public Integer getFemaleChild() {
        return femaleChild;
    }

    public void setFemaleChild(Integer femaleChild) {
        this.femaleChild = femaleChild;
    }

    public Integer getDisplaced() {
        return displaced;
    }

    public void setDisplaced(Integer displaced) {
        this.displaced = displaced;
    }

    public Integer getItns() {
        return itns;
    }

    public void setItns(Integer itns) {
        this.itns = itns;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSubmissionUuid() {
        return submissionUuid;
    }

    public void setSubmissionUuid(String submissionUuid) {
        this.submissionUuid = submissionUuid;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public String getHouseUuid() {
        return houseUuid;
    }

    public void setHouseUuid(String houseUuid) {
        this.houseUuid = houseUuid;
    }

    public ItnsVillageDTO getVillageData() {
        return villageData;
    }

    public void setVillageData(ItnsVillageDTO villageData) {
        this.villageData = villageData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItnsVillageHousesDetailDTO)) {
            return false;
        }

        ItnsVillageHousesDetailDTO itnsVillageHousesDetailDTO = (ItnsVillageHousesDetailDTO) o;
        if (this.uid == null) {
            return false;
        }
        return Objects.equals(this.uid, itnsVillageHousesDetailDTO.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uid);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ItnsVillageHousesDetailDTO{" +
            "uid=" + getUid() +
            ", code='" + getCode() + "'" +
            ", couponId=" + getCouponId() +
            ", name='" + getName() + "'" +
            ", male=" + getMale() +
            ", female=" + getFemale() +
            ", pregnant=" + getPregnant() +
            ", population=" + getPopulation() +
            ", maleChild=" + getMaleChild() +
            ", femaleChild=" + getFemaleChild() +
            ", displaced=" + getDisplaced() +
            ", itns=" + getItns() +
            ", comment='" + getComment() + "'" +
            ", submissionUuid='" + getSubmissionUuid() + "'" +
            ", deleted='" + getDeleted() + "'" +
            ", houseUuid='" + getHouseUuid() + "'" +
            ", villageData=" + getVillageData() +
            "}";
    }
}
