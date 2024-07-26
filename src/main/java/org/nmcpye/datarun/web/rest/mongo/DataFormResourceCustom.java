package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.drun.mongo.domain.DataForm;
import org.nmcpye.datarun.drun.mongo.repository.DataFormRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.DataFormServiceCustom;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link DataForm}.
 */
@RestController
@RequestMapping("/api/custom/dataForms")
public class DataFormResourceCustom extends AbstractMongoResource<DataForm> {

    public DataFormResourceCustom(DataFormServiceCustom dataFormServiceCustom,
                                  DataFormRepositoryCustom dataFormRepositoryCustom) {
        super(dataFormServiceCustom, dataFormRepositoryCustom);
    }

    @Override
    protected String getName() {
        return "dataForms";
    }
}
