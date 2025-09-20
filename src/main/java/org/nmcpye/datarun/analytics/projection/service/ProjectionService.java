package org.nmcpye.datarun.analytics.projection.service;

import java.util.List;

/**
 * @author Hamza Assada
 * @since 18/09/2025
 */
public interface ProjectionService {
    void runForRepeat(String repeatUid) throws Exception;
    void processRepeatForPaths(String repeatUid, List<String> semanticPaths, int pageSize) throws Exception;
}
