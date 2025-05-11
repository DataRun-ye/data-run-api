package org.nmcpye.datarun.dto.team;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link org.nmcpye.datarun.drun.postgres.domain.Team}
 */
@Value
public class TeamDto implements Serializable {
    @Size(max = 11)
    String uid;
    @NotNull
    String code;
}
