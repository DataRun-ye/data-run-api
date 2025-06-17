package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.team.Team}
 */
@AllArgsConstructor
@Getter
@Setter
public class TeamDto extends AbstractBaseDto {
    @Size(max = 11)
    private final String uid;
    private final String code;
    private final String name;
    private final String description;
    private final Boolean disabled;
    private final Instant enabledFrom;
    private final Instant enabledTo;
}
