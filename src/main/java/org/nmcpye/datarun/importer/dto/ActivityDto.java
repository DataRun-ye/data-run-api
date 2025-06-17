package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.activity.Activity}
 */
@AllArgsConstructor
@Getter
@Setter
public class ActivityDto extends AbstractBaseDto {
    @Size(max = 11)
    private final String uid;
    private final String code;
    private final String name;
    private final Instant startDate;
    private final Instant endDate;
    private final Boolean disabled;
}
