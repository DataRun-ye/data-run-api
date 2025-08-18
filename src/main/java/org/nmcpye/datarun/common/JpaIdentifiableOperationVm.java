package org.nmcpye.datarun.common;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObject;

import java.util.List;

/**
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */
@Builder
@Value
public class JpaIdentifiableOperationVm<T extends JpaIdentifiableObject> {
    @Singular
    List<T> forCreatEntities;
    @Singular
    List<T> forUpdateEntities;
}
