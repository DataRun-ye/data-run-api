package org.nmcpye.datarun.mongo.service;


import org.nmcpye.datarun.common.IdentifiableService;
import org.nmcpye.datarun.mongo.domain.DataForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Custom Interface for managing {@link DataForm}.
 */
public interface DataFormService extends IdentifiableService<DataForm, String> {
    Page<DataForm> getAccessibleForms(Pageable pageable);
}
