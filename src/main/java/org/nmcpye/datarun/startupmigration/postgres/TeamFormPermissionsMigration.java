package org.nmcpye.datarun.startupmigration.postgres;

import jakarta.persistence.EntityManager;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.TeamRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Hamza Assada, 24/04/2025
 */
//@Component
//@Transactional
public class TeamFormPermissionsMigration implements CommandLineRunner {
    private static final int CHUNK_SIZE = 400;
    final TeamRepository repository;

    private final EntityManager em;
    private final PlatformTransactionManager txm;


    public TeamFormPermissionsMigration(TeamRepository repository, EntityManager em, PlatformTransactionManager txm) {
        this.repository = repository;
        this.em = em;
        this.txm = txm;
    }

    @Override
    public void run(String... args) throws Exception {
        processInChunks();
    }

    private void processInChunks() {
        int page = 0;
        Page<Team> chunk;
        do {
            Pageable pg = PageRequest.of(page, CHUNK_SIZE);
            chunk = repository.findAll(pg);

            if (!chunk.isEmpty()) {
                // run each chunk in its own transaction to keep EM small
                Page<Team> finalChunk = chunk;
                new TransactionTemplate(txm).execute(status -> {
                    for (Team team : finalChunk) {
                        // calling getter will recompute
                        team.setFormPermissions(team.getFormPermissions());
                    }
                    repository.saveAll(finalChunk.getContent());
                    // make sure we don’t accumulate managed state
                    em.flush();
                    em.clear();
                    return null;
                });
            }
            page++;
        } while (!chunk.isLast());
    }
}
