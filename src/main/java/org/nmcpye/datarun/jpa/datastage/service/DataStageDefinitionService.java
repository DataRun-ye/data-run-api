package org.nmcpye.datarun.jpa.datastage.service;


import org.nmcpye.datarun.jpa.common.JpaAuditableObjectService;
import org.nmcpye.datarun.jpa.datastage.DataStageDefinition;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;

/**
 * Service Custom Interface for managing {@link DataTemplate}.
 */
public interface DataStageDefinitionService
    extends JpaAuditableObjectService<DataStageDefinition> {
}
