package org.nmcpye.datarun.jpa.flowrun.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.nmcpye.datarun.jpa.flowrun.FlowRun;

import java.util.List;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 */
public class FlowRunRepositoryWithBagRelationshipsImpl
    implements FlowRunRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String ORG_UNITS_PARAMETER = "assignments";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void updatePaths() {
        List<FlowRun> flowRuns = entityManager
            .createQuery(
                "select assignment from FlowRun assignment " +
                    "where assignment.path is null or assignment.hierarchyLevel is null",
                FlowRun.class
            )
            .getResultList();

        updatePaths(flowRuns);
    }

    @Override
    public void forceUpdatePaths() {
        List<FlowRun> flowRuns = entityManager
            .createQuery(
                "select assignment from FlowRun assignment ",
                FlowRun.class
            )
            .getResultList();

        updatePaths(flowRuns);
    }

    private void updatePaths(List<FlowRun> flowRuns) {

        int counter = 0;

        for (FlowRun flowRun : flowRuns) {
            flowRun.setPath(flowRun.getPath());
            flowRun.setHierarchyLevel(flowRun.getHierarchyLevel());

            entityManager.merge(flowRun);

            if ((counter % 400) == 0) {
                entityManager.flush();
            }

            counter++;
        }
    }
}
