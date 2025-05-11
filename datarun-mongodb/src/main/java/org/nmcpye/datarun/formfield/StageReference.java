package org.nmcpye.datarun.formfield;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
public class StageReference {
    private String stage;

    @Field("type")
    private String element;

    private Boolean isMulti = false;
}
