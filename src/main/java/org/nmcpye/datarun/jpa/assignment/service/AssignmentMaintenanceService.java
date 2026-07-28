package org.nmcpye.datarun.jpa.assignment.service;

import jakarta.persistence.EntityManager;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Hamza Assada 20/03/2023
 */
@Service
public class AssignmentMaintenanceService {
    private static final int CHUNK_SIZE = 400;

    private final AssignmentRepository repository;
    private final EntityManager em;
    private final PlatformTransactionManager txm;

    public AssignmentMaintenanceService(AssignmentRepository repository,
                                        EntityManager em,
                                        PlatformTransactionManager txm) {
        this.repository = repository;
        this.em = em;
        this.txm = txm;
    }

    public void updateMissingPaths() {
        processInChunks(false);
    }

    public void forceRecomputePaths() {
        processInChunks(true);
    }

    private void processInChunks(boolean force) {
        int page = 0;
        while (true) {
            Pageable pg = PageRequest.of(
                force ? page : 0,
                CHUNK_SIZE,
                Sort.by("id").ascending()
            );
            Page<Assignment> chunk = force
                ? repository.findAll(pg)
                : repository.findAllByPathIsNull(pg);
            if (chunk.isEmpty()) {
                return;
            }
            Page<Assignment> finalChunk = chunk;
            new TransactionTemplate(txm).execute(status -> {
                for (Assignment assignment : finalChunk) {
                    String path = assignment.getPath();
                    Integer hierarchyLevel = assignment.getHierarchyLevel();
                    em.detach(assignment);
                    repository.updateDerivedPath(
                        assignment.getId(),
                        path,
                        hierarchyLevel
                    );
                }
                em.flush();
                em.clear();
                return null;
            });
            if (force) {
                if (chunk.isLast()) {
                    return;
                }
                page++;
            }
        }
    }
}
