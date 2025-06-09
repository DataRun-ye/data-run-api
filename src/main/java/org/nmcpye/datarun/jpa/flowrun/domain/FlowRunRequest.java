package org.nmcpye.datarun.jpa.flowrun.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @author Hamza Assada 08/06/2025 <7amza.it@gmail.com>
 */
@Getter
@Setter
public class FlowRunRequest {
    @Size(max = 11)
    private String uid;
    /**
     * later: uid, or flowTypeCode depending on identifier schema
     */
    @NotBlank
    private String flowTypeUid;

    @NotNull
    private Map<String, String> scopes;
}
