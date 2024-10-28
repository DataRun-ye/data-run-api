package org.nmcpye.datarun.drun.postgres.dto;

import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * DTO for {@link org.nmcpye.datarun.domain.Activity}
 */
@lombok.Value
public class ActivityDto implements Serializable {
    Long id;
    @Size(max = 11)
    String uid;
    String code;
    String name;
}
