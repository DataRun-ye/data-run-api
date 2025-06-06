package org.nmcpye.datarun.importer.service;

import org.nmcpye.datarun.importer.config.ImportRequest;
import org.nmcpye.datarun.importer.config.ImportResult;

/**
 * a service that routes the import request to the appropriate processor
 * based on the entity type,a condition,...etc. and controlling the flow
 *
 * @author Hamza Assada 04/06/2025 <7amza.it@gmail.com>
 */
public interface EntityImportService {
    <T> ImportResult<T> importEntity(ImportRequest request);
}
