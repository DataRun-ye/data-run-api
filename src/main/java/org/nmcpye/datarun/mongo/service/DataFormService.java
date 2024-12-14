package org.nmcpye.datarun.mongo.service;


import org.nmcpye.datarun.mongo.domain.DataForm;

import java.util.List;

/**
 * Service Custom Interface for managing {@link DataForm}.
 */
public interface DataFormService
    extends IdentifiableMongoService<DataForm> {

    List<DataForm> findAllByActivity(String uid);
}
