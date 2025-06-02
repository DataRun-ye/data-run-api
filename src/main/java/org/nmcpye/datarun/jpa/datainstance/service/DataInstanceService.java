package org.nmcpye.datarun.jpa.datainstance.service;

import org.nmcpye.datarun.common.SoftDeleteService;
import org.nmcpye.datarun.jpa.datainstance.DataInstance;

public interface DataInstanceService
    extends SoftDeleteService<DataInstance, Long> {
}
