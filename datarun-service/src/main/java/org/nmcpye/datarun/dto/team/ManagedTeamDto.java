package org.nmcpye.datarun.dto.team;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.io.Serializable;

@Value
public class ManagedTeamDto implements Serializable {
    @JsonProperty(value = "id")
    @Size(max = 11)
    String uid;

    @NotNull
    String code;

    @JsonProperty(value = "activity")
    String activityUid;

    String managedBy;
}
