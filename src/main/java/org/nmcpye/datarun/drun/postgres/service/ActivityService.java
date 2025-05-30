package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.common.jpa.JpaAuditableObjectService;
import org.nmcpye.datarun.activity.Activity;

/**
 * Service Interface for managing {@link Activity}.
 */
public interface ActivityService
    extends JpaAuditableObjectService<Activity> {
}
