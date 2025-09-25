package org.nmcpye.datarun.web.rest.v1.outbox;

import org.nmcpye.datarun.jpa.datasubmissionoutbox.AdminOutboxService;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Hamza Assada
 * @since 24/09/2025
 */
@RestController
@RequestMapping("/admin/outbox")
public class AdminOutboxController {

    private final AdminOutboxService service;

    public AdminOutboxController(AdminOutboxService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<OutboxEvent>> listByStatus(@RequestParam String status, @RequestParam(defaultValue = "50") int limit, @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(service.listByStatus(status.toUpperCase(), limit, offset));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OutboxEvent> getById(@PathVariable long id) {
        OutboxEvent e = service.getById(id);
        if (e == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(e);
    }

    @PostMapping("/{id}/requeue")
    public ResponseEntity<?> requeue(@PathVariable long id) {
        boolean ok = service.requeue(id);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<?> release(@PathVariable long id) {
        boolean ok = service.releaseClaim(id);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/retry-now")
    public ResponseEntity<?> retryNow(@PathVariable long id) {
        boolean ok = service.forceRetryNow(id);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}

