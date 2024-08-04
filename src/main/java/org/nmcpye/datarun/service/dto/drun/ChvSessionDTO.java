package org.nmcpye.datarun.service.dto.drun;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.domain.enumeration.MSessionSubject;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link org.nmcpye.datarun.domain.ChvSession} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ChvSessionDTO implements Serializable {

    @Size(max = 11)
    private String uid;

    private String code;

    private String name;

    @NotNull
    private Instant sessionDate;

    private MSessionSubject subject;

    @NotNull
    private Integer sessions;

    @NotNull
    private Integer peopleItns;

    private String comment;

    private Instant startEntryTime;

    private Boolean deleted;

    private TeamDTO team;

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

    public Instant getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(Instant sessionDate) {
        this.sessionDate = sessionDate;
    }

    public MSessionSubject getSubject() {
        return subject;
    }

    public void setSubject(MSessionSubject subject) {
        this.subject = subject;
    }

    public Integer getSessions() {
        return sessions;
    }

    public void setSessions(Integer sessions) {
        this.sessions = sessions;
    }

    public Integer getPeopleItns() {
        return peopleItns;
    }

    public void setPeopleItns(Integer peopleItns) {
        this.peopleItns = peopleItns;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getStartEntryTime() {
        return startEntryTime;
    }

    public void setStartEntryTime(Instant startEntryTime) {
        this.startEntryTime = startEntryTime;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public TeamDTO getTeam() {
        return team;
    }

    public void setTeam(TeamDTO team) {
        this.team = team;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChvSessionDTO)) {
            return false;
        }

        ChvSessionDTO chvSessionDTO = (ChvSessionDTO) o;
        if (this.uid == null) {
            return false;
        }
        return Objects.equals(this.uid, chvSessionDTO.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uid);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ChvSessionDTO{" +
            "uid=" + getUid() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", sessionDate='" + getSessionDate() + "'" +
            ", subject='" + getSubject() + "'" +
            ", sessions=" + getSessions() +
            ", people=" + getPeopleItns() +
            ", comment='" + getComment() + "'" +
            ", startEntryTime='" + getStartEntryTime() + "'" +
            ", deleted='" + getDeleted() + "'" +
            ", team=" + getTeam() +
            "}";
    }

    public ActivityDTO getActivity() {
        return activity;
    }

    public void setActivity(ActivityDTO activity) {
        this.activity = activity;
    }
}
