package org.nmcpye.datarun.jpa.entityattribute;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.jpa.entityType.EntityType;

/**
 * an {@link EntityAttributeInstance} links an {@link EntityType} to an {@link EntityAttributeType}
 * and configure it with additional configuration properties
 *
 * @author Hamza Assada 29/05/2025 <7amza.it@gmail.com>
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
}
