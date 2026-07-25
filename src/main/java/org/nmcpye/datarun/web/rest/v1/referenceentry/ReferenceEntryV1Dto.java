package org.nmcpye.datarun.web.rest.v1.referenceentry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceEntryV1Dto implements Serializable {
    private String uid;
    private String name;
    private String orgUnitUid;
}
