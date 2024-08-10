package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.drun.mongo.domain.DataForm;
import org.nmcpye.datarun.drun.mongo.repository.DataFormRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.DataFormServiceCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing {@link DataForm}.
 */
@RestController
@RequestMapping("/api/custom/dataForms")
public class DataFormResourceCustom extends AbstractMongoResource<DataForm> {

    final DataFormServiceCustom dataFormServiceCustom;

    public DataFormResourceCustom(DataFormServiceCustom dataFormServiceCustom,
                                  DataFormRepositoryCustom dataFormRepositoryCustom) {
        super(dataFormServiceCustom, dataFormRepositoryCustom);
        this.dataFormServiceCustom = dataFormServiceCustom;
    }

    @Override
    protected Page<DataForm> getList(Pageable pageable, boolean eagerload) {
        List<DataForm> dataForms = dataFormServiceCustom.findAllByUser();
        if (!pageable.isPaged()) {
            return new PageImpl<>(dataForms);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), dataForms.size());
        if (start > end) {
            return Page.empty(pageable);
        }

        List<DataForm> sublist = dataForms.subList(start, end);
        return new PageImpl<>(sublist, pageable, dataForms.size());
    }


    @Override
    protected String getName() {
        return "dataForms";
    }
}
