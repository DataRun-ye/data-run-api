package org.nmcpye.datarun.jpa.flowinstance.dto;

/**
 * DTO: Flow completion status by team
 * @author Hamza Assada 10/06/2025 <7amza.it@gmail.com>
 */
public interface FlowStatusByTeamReport {
    String getTeamId();
    String getFlowType();
    long getCompletedCount();
    long getInProgressCount();
    long getErrorCount();
}
