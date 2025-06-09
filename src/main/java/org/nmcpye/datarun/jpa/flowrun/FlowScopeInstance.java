package org.nmcpye.datarun.jpa.flowrun;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.jpa.flowtype.ScopePropertyType;

/**
 * @author Hamza Assada 09/06/2025 <7amza.it@gmail.com>
 */
@Getter
@Setter
@NoArgsConstructor
public class FlowScopeInstance {
    @NotNull
    private ScopePropertyType type;
    @NotNull
    private String value;
}
