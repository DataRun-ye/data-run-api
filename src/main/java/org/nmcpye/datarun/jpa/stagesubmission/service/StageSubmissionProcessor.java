package org.nmcpye.datarun.jpa.stagesubmission.service;

import org.nmcpye.datarun.jpa.stagesubmission.StageInstance;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * @author Hamza Assada 11/06/2025 <7amza.it@gmail.com>
 */
public interface StageSubmissionProcessor {
    @Transactional
    StageInstance processSubmission(String flowInstanceId,
                                    String stageId,
                                    Map<String, Object> data,
                                    boolean isRepeatable,
                                    String boundEntityTypeId);
}
