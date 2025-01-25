package org.nmcpye.datarun.mongo.service;


import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;

import java.util.List;

/**
 * Service Custom Interface for managing {@link DataFormTemplate}.
 */
public interface DataFormTemplateService
    extends IdentifiableMongoService<DataFormTemplate> {
    List<DataFormTemplate> findTemplatesByFieldType(ValueType type);
}
