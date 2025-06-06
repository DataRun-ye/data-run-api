package org.nmcpye.datarun.importprocessor;

/**
 * @author Hamza Assada 14/05/2025 <7amza.it@gmail.com>
 */
public interface ImportStage<T> {
    void process(ImportContext<T> context);
}
