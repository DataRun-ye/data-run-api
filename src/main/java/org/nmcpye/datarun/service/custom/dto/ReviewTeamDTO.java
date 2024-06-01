package org.nmcpye.datarun.service.custom.dto;

import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link org.nmcpye.datarun.domain.ReviewTeam} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReviewTeamDTO implements Serializable {

    @Size(max = 11)
    private String uid;

    private String code;

    private String name;

    private String user;

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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReviewTeamDTO)) {
            return false;
        }

        ReviewTeamDTO reviewTeamDTO = (ReviewTeamDTO) o;
        if (this.uid == null) {
            return false;
        }
        return Objects.equals(this.uid, reviewTeamDTO.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uid);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReviewTeamDTO{" +
            "uid=" + getUid() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", user='" + getUser() + "'" +
            "}";
    }
}
