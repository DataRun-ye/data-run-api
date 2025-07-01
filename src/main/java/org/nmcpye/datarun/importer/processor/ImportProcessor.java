package org.nmcpye.datarun.importer.processor;

import org.nmcpye.datarun.importer.config.ImportRequest;
import org.nmcpye.datarun.importer.config.ImportResult;

/**
 * @author Hamza Assada 04/06/2025 (7amza.it@gmail.com)
 */
public interface ImportProcessor<T> {
    String getEntityType();

    ImportResult<T> process(ImportRequest request);
}
