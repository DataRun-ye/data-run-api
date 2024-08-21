package org.nmcpye.datarun.drun.mongo.service;


import org.nmcpye.datarun.drun.mongo.domain.DataForm;

import java.util.List;

/**
 * Service Custom Interface for managing {@link DataForm}.
 */
public interface DataFormServiceCustom
    extends IdentifiableMongoService<DataForm> {

    List<DataForm> findAllByActivity(String uid);
}
