package org.nmcpye.datarun.jpa.migration;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/// @author Hamza Assada
/// @since 24/04/2025
@Component
@Transactional
@Slf4j
public class AssignmentFormsMigration /*implements CommandLineRunner*/ {
    private static final int CHUNK_SIZE = 400;
    final AssignmentRepository repository;

    private final EntityManager em;
    private final PlatformTransactionManager txm;


    public AssignmentFormsMigration(AssignmentRepository repository, EntityManager em, PlatformTransactionManager txm) {
        this.repository = repository;
        this.em = em;
        this.txm = txm;
    }

//    @Override
    public void run(String... args) throws Exception {
        processInChunks();
    }

    public void processInChunks() {
        int page = 0;
        Page<Assignment> chunk;
        do {
            Pageable pg = PageRequest.of(page, CHUNK_SIZE);
            chunk = repository.findAll(pg);

            if (!chunk.isEmpty()) {
                // run each chunk in its own transaction to keep EM small
                Page<Assignment> finalChunk = chunk;
                new TransactionTemplate(txm).execute(status -> {
                    for (Assignment flowInstance : finalChunk) {
                        flowInstance.setForms(flowInstance.getForms());
                    }
                    repository.saveAll(finalChunk.getContent());

                    em.flush();
                    em.clear();
                    return null;
                });
            }
            log.info("saving next {} assignments", page);
            page++;
        } while (!chunk.isLast());
    }
}
