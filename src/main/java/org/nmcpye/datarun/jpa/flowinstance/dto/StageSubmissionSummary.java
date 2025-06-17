package org.nmcpye.datarun.jpa.flowinstance.dto;

import java.time.Instant;

/**
 * DTO: Summary of StageSubmission per FlowInstance
 *
 * @author Hamza Assada 10/06/2025 <7amza.it@gmail.com>
 */
public interface StageSubmissionSummary {
    String getFlowInstanceId();

    String getStageName();

    Instant CreatedDate();

    String getSubmittedBy();

    String getStatus();
}
