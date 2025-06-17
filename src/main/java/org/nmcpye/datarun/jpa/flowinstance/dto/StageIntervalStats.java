package org.nmcpye.datarun.jpa.flowinstance.dto;

/**
 * DTO: Average time between stages
 *
 * @author Hamza Assada 10/06/2025 <7amza.it@gmail.com>
 */
public interface StageIntervalStats {
    String getFlowType();

    double getAverageIntervalSeconds();
}
