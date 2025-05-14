package org.nmcpye.datarun.importprocessor;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <14-05-2025>
 */
public interface ImportStage<T> {
    void process(ImportContext<T> context);
}
