package org.nmcpye.datarun.formtemplate;


import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.mongo.domain.DataForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Custom Interface for managing {@link DataForm}.
 */
public interface DataFormService extends AuditableObjectService<DataForm, String> {
    Page<DataForm> getAccessibleForms(Pageable pageable);
}
