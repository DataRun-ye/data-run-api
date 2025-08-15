package org.nmcpye.datarun.jpa.etl.service;

import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;

/**
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */
public interface DataNormalizer {
    NormalizedSubmission normalize(DataFormSubmission submission);
}
