package org.nmcpye.datarun.jpa.activity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;

import java.time.Instant;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.activity.Activity}
 *
 * @author Hamza Assada 02/04/2025 <7amza.it@gmail.com>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ActivityDto extends BaseDto {
    private String name;
    private Instant startDate;
    private Instant endDate;
    @NotNull
    private BaseDto project;
}
