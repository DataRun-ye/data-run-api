package org.nmcpye.datarun.activity;

import org.nmcpye.datarun.common.jpa.JpaAuditableObjectService;
import org.nmcpye.datarun.domain.Activity;

/**
 * Service Interface for managing {@link Activity}.
 */
public interface ActivityService
    extends JpaAuditableObjectService<Activity> {
}
