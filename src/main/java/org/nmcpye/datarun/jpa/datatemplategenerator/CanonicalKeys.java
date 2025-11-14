package org.nmcpye.datarun.jpa.datatemplategenerator;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import org.nmcpye.datarun.jpa.datatemplate.DataType;
import org.nmcpye.datarun.jpa.datatemplate.SemanticType;

@Builder
@Value
@Accessors(fluent = true)
public class CanonicalKeys {
    String templateUid;
    String canonicalPath;
    DataType dataType;
    SemanticType semanticType;
    String cardinality;
    String optionSetUid;
}
