package org.nmcpye.datarun.service.custom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.domain.enumeration.Gender;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link org.nmcpye.datarun.domain.PatientInfo} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PatientInfoDTO implements Serializable {

    @Size(max = 11)
    private String uid;

    private String code;

    private String name;

    @Min(value = 1)
    @Max(value = 140)
    private Integer age;

    private Gender gender;

    private AssignmentDTO location;

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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public AssignmentDTO getLocation() {
        return location;
    }

    public void setLocation(AssignmentDTO location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PatientInfoDTO)) {
            return false;
        }

        PatientInfoDTO patientInfoDTO = (PatientInfoDTO) o;
        if (this.uid == null) {
            return false;
        }
        return Objects.equals(this.uid, patientInfoDTO.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uid);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PatientInfoDTO{" +
            "uid=" + getUid() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", age=" + getAge() +
            ", gender='" + getGender() + "'" +
            ", location=" + getLocation() +
            "}";
    }
}
