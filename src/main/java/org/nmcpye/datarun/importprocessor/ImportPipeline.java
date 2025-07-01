package org.nmcpye.datarun.importprocessor;

import java.util.List;

/**
 * @author Hamza Assada 14/05/2025 (7amza.it@gmail.com)
 */
public interface ImportPipeline<T> {
    ImportResult run(List<T> input, boolean dryRun);
}
