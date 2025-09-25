package org.nmcpye.datarun.jpa.datasubmissionoutbox;

import org.nmcpye.datarun.jpa.datasubmissionoutbox.repository.OutboxAdminRepository;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * @author Hamza Assada
 * @since 24/09/2025
 */
@Service
public class AdminOutboxService {


    private final OutboxAdminRepository repo;


    public AdminOutboxService(OutboxAdminRepository repo) {
        this.repo = repo;
    }


    public List<OutboxEvent> listByStatus(String status, int limit, int offset) {
        return repo.listByStatus(status, limit, offset);
    }


    public OutboxEvent getById(long id) {
        return repo.findById(id);
    }


    public boolean requeue(long id) {
        return repo.requeue(id) > 0;
    }


    public boolean releaseClaim(long id) {
        return repo.releaseClaim(id) > 0;
    }


    public boolean forceRetryNow(long id) {
        return repo.forceRetryNow(id) > 0;
    }
}

