package org.nmcpye.datarun.etl.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nmcpye.datarun.jpa.datatemplate.CanonicalElement;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class TemplateContext implements Serializable {
    @NotNull
    @Size(max = 11)
    private String templateUid;

    @Builder.Default
    private Map<String, CanonicalElement> allCanonicalElementsMap = new HashMap<>();

    @Builder.Default
    private Map<String, CanonicalElement> repeatCanonicalElementsMap = new HashMap<>();

//    @Builder.Default
//    private Map<String, CanonicalElementAnchorDto> anchorsMap = new HashMap<>();
}
