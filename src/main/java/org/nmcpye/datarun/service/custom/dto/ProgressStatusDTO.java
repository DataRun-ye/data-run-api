package org.nmcpye.datarun.service.custom.dto;

import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link org.nmcpye.datarun.domain.ProgressStatus} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ProgressStatusDTO implements Serializable {

    @Size(max = 11)
    private String uid;

    private String code;

    private String name;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProgressStatusDTO)) {
            return false;
        }

        ProgressStatusDTO progressStatusDTO = (ProgressStatusDTO) o;
        if (this.uid == null) {
            return false;
        }
        return Objects.equals(this.uid, progressStatusDTO.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uid);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ProgressStatusDTO{" +
            "uid=" + getUid() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            "}";
    }
}
