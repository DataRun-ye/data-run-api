package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.domain.ChvRegister;
import org.nmcpye.datarun.service.ChvRegisterService;
import org.nmcpye.datarun.service.dto.drun.SaveSummaryOld;

/**
 * Service Interface for managing {@link ChvRegister}.
 */
public interface ChvRegisterServiceCustom
    extends IdentifiableService<ChvRegister>, ChvRegisterService {

    SaveSummaryOld saveWithReferences(ChvRegister chvRegister);
}
