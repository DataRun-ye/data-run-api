package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.postgres.common.ActivitySpecifications;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.springframework.data.jpa.domain.Specification;

/**
 * Service Interface for managing {@link Activity}.
 */
public interface ActivityServiceCustom
    extends IdentifiableRelationalService<Activity> {
    @Override
    default Specification<Activity> canRead() {
        return ActivitySpecifications.canRead();
    }

}
