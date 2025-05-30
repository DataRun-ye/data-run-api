package org.nmcpye.datarun.drun.postgres.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;
import org.nmcpye.datarun.team.Team;

import java.io.Serializable;

/**
 * DTO for {@link Team}
 */
@Value
public class TeamDto implements Serializable {
    @Size(max = 11)
    String uid;
    @NotNull
    String code;
}
