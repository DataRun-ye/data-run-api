package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.drun.mongo.domain.DataForm;
import org.nmcpye.datarun.drun.mongo.repository.DataFormRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.DataFormService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link DataForm}.
 */
@RestController
@RequestMapping("/api/custom/dataForms")
public class DataFormResourceCustom extends AbstractMongoResource<DataForm> {

    final DataFormService dataFormService;

    public DataFormResourceCustom(DataFormService dataFormService,
                                  DataFormRepositoryCustom dataFormRepositoryCustom) {
        super(dataFormService, dataFormRepositoryCustom);
        this.dataFormService = dataFormService;
    }

    @Override
    protected String getName() {
        return "dataForms";
    }
}
