package org.nmcpye.datarun.jpa.datavalue;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class InstanceElementDataValue implements Serializable {

    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    @Serial
    private static final long serialVersionUID = 2738519623273453182L;

    @NotNull
    private String dataElementUid;
    private String value;
    private Boolean providedElsewhere = false;
    private Instant createdDate = Instant.now();
    private Instant lastModifiedDate = Instant.now();
    private String createdBy;
}
