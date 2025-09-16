package org.nmcpye.datarun.common.uidgenerate;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author Hamza Assada 09/06/2025 (7amza.it@gmail.com)
 */
@Data
@EqualsAndHashCode
public class BaseDto implements Serializable {
    @Size(max = 26)
    protected String id;
    protected String code;
}
