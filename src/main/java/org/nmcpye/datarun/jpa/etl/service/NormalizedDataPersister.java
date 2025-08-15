package org.nmcpye.datarun.jpa.etl.service;

import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */
public interface NormalizedDataPersister {
    @Transactional
    void persist(NormalizedSubmission ns);
}
