package org.nmcpye.etl.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Actor {
    private String uid;
    private String code;
    private String name;
    private String actorType;
}
