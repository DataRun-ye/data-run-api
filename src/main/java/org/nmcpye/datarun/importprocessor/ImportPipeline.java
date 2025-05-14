package org.nmcpye.datarun.importprocessor;

import java.util.List;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <14-05-2025>
 */
public interface ImportPipeline<T> {
    ImportResult run(List<T> input, boolean dryRun);
}
