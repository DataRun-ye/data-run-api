package org.nmcpye.datarun.web.rest;

import org.nmcpye.datarun.domain.ProgressStatus;
import org.nmcpye.datarun.drun.postgres.repository.ProgressStatusRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ProgressStatusServiceCustom;
import org.nmcpye.datarun.web.rest.common.AbstractResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link ProgressStatus}.
 */
@RestController
@RequestMapping("/api/custom/progressStatuses")
public class ProgressStatusResourceCustom
    extends AbstractResource<ProgressStatus> {

    private final ProgressStatusServiceCustom progressStatusService;

    private final ProgressStatusRepositoryCustom progressStatusRepository;

    public ProgressStatusResourceCustom(ProgressStatusServiceCustom progressStatusService,
                                        ProgressStatusRepositoryCustom progressStatusRepository) {
        super(progressStatusService, progressStatusRepository);
        this.progressStatusService = progressStatusService;
        this.progressStatusRepository = progressStatusRepository;
    }

    @Override
    protected Page<ProgressStatus> getList(Pageable pageable, boolean eagerload) {
        return progressStatusService.findAll(pageable);
    }

    @Override
    protected String getName() {
        return "progressStatuses";
    }
}
