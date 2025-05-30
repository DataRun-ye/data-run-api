package org.nmcpye.datarun.assignment.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.nmcpye.datarun.assignment.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 */
public class AssignmentRepositoryWithBagRelationshipsImpl
    implements AssignmentRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String ORG_UNITS_PARAMETER = "assignments";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void updatePaths() {
        List<Assignment> assignments = entityManager
            .createQuery(
                "select assignment from Assignment assignment " +
                    "where assignment.path is null or assignment.hierarchyLevel is null",
                Assignment.class
            )
            .getResultList();

        updatePaths(assignments);
    }

    @Override
    public void forceUpdatePaths() {
        List<Assignment> assignments = entityManager
            .createQuery(
                "select assignment from Assignment assignment ",
                Assignment.class
            )
            .getResultList();

        updatePaths(assignments);
    }

    private void updatePaths(List<Assignment> assignments) {

        int counter = 0;

        for (Assignment assignment : assignments) {
            assignment.setPath(assignment.getPath());
            assignment.setHierarchyLevel(assignment.getHierarchyLevel());

            entityManager.merge(assignment);

            if ((counter % 400) == 0) {
                entityManager.flush();
            }

            counter++;
        }
    }


    @Override
    public Optional<Assignment> fetchBagRelationships(Assignment assignment) {
        return Optional.ofNullable(assignment).map(this::fetchAssignments);
    }

    @Override
    public Page<Assignment> fetchBagRelationships(Page<Assignment> assignments) {
        return new PageImpl<>(fetchBagRelationships(assignments.getContent()), assignments.getPageable(), assignments.getTotalElements());
    }

    @Override
    public List<Assignment> fetchBagRelationships(List<Assignment> assignments) {
        return Optional.of(assignments).map(this::fetchAssignments).orElse(Collections.emptyList());
    }

    Assignment fetchAssignments(Assignment result) {
        return entityManager
            .createQuery(
                "select assignment from Assignment assignment " +
                    "left join fetch assignment.assignmentForms ass " +
                    "where assignment.id = :id",
                Assignment.class
            )
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<Assignment> fetchAssignments(List<Assignment> assignments) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, assignments.size()).forEach(index -> order.put(assignments.get(index).getId(), index));
        List<Long> ids = entityManager
            .createQuery(
                "select assignment.id from Assignment assignment " +
                    "where assignment in :assignments",
                Long.class
            )
            .setParameter(ORG_UNITS_PARAMETER, assignments)
            .getResultList();

        List<Assignment> result = entityManager
            .createQuery(
                "select assignment from Assignment assignment " +
                    "join fetch assignment.assignmentForms " +
                    "where assignment.id in (:ids)",
                Assignment.class
            )
            .setParameter("ids", ids)
            .getResultList();
        result.sort((o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
