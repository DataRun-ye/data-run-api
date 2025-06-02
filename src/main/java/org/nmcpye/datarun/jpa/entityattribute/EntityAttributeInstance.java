package org.nmcpye.datarun.jpa.entityattribute;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.jpa.entityType.EntityType;

/**
 * an {@link EntityAttributeInstance} links an {@link EntityType} to an {@link EntityAttributeType}
 * and configure it with additional configuration properties
 *
 * @author Hamza Assada, <7amza.it@gmail.com> <29-05-2025>
 */
@Getter
@Setter
@NoArgsConstructor
public class EntityAttributeInstance
        extends EntityAttributeType {

    private Boolean displayInList = false;

    private Boolean mandatory = false;

    private Boolean searchable = false;

    private String entityTypeUid;

    @JsonIgnore
    @Override
    public Long getId() {
        return super.getId();
    }
}
