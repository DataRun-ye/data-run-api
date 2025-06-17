package org.nmcpye.datarun.jpa.flowinstance.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;

import java.util.List;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 *
 * @author Hamza Assada 20/03/2023
 */
public class FlowInstanceRepositoryWithBagRelationshipsImpl
    implements FlowInstanceRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String ORG_UNITS_PARAMETER = "assignments";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void updatePaths() {
        List<FlowInstance> flowInstances = entityManager
            .createQuery(
                "select assignment from FlowInstance assignment " +
                    "where assignment.path is null or assignment.hierarchyLevel is null",
                FlowInstance.class
            )
            .getResultList();

        updatePaths(flowInstances);
    }

    @Override
    public void forceUpdatePaths() {
        List<FlowInstance> flowInstances = entityManager
            .createQuery(
                "select assignment from FlowInstance assignment ",
                FlowInstance.class
            )
            .getResultList();

        updatePaths(flowInstances);
    }

    private void updatePaths(List<FlowInstance> flowInstances) {

        int counter = 0;

        for (FlowInstance flowInstance : flowInstances) {
            flowInstance.setPath(flowInstance.getPath());
            flowInstance.setHierarchyLevel(flowInstance.getHierarchyLevel());

            entityManager.merge(flowInstance);

            if ((counter % 400) == 0) {
                entityManager.flush();
            }

            counter++;
        }
    }
}
