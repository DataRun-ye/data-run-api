package org.nmcpye.datarun.mapper.dto;

import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link org.nmcpye.datarun.drun.postgres.common.translation.Translation}
 */
@Value
public class TranslationDto implements Serializable {
    String locale;
    String property;
    String value;
}
