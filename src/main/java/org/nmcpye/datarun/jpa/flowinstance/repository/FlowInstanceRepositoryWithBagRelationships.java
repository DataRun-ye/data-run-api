package org.nmcpye.datarun.jpa.flowinstance.repository;

/**
 * @author Hamza Assada 20/03/2023
 */
public interface FlowInstanceRepositoryWithBagRelationships {
    void updatePaths();

    void forceUpdatePaths();

}
