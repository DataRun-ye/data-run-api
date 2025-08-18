package org.nmcpye.datarun.web.rest.postgres;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObject;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObjectService;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.web.rest.common.BaseReadWriteResource;

@Slf4j
public abstract class JpaBaseResource<T extends JpaIdentifiableObject>
    extends BaseReadWriteResource<T, String> {
    protected final JpaIdentifiableObjectService<T> jpaAuditableObjectService;
    protected final JpaIdentifiableRepository<T> jpaIdentifiableRepository;

    protected JpaBaseResource(JpaIdentifiableObjectService<T> jpaAuditableObjectService,
                              JpaIdentifiableRepository<T> repository) {
        super(jpaAuditableObjectService, repository);
        this.jpaAuditableObjectService = jpaAuditableObjectService;
        this.jpaIdentifiableRepository = repository;
    }

    //    @GetMapping(value = "/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
//    public ResponseEntity<StreamingResponseBody> streamAssignments(
//        @RequestParam(required = false) Instant since) {
//
//        Instant cutoff = since != null ? since : Instant.EPOCH;
//        StreamingResponseBody body = out -> {
//            jpaIdentifiableRepository.streamByLastModifiedDateAfter(cutoff)
//                .forEach(entity -> {
//                    String line = null;
//                    try {
//                        line = objectMapper.writeValueAsString(entity) + "\n";
//                        out.write(line.getBytes(StandardCharsets.UTF_8));
//                        out.flush();  // push each record immediately
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                });
//        };
//
//        // You can still send a Last-Modified header based on max timestamp
//        Instant maxTs = jpaIdentifiableRepository.findMaxLastModifiedAfter(cutoff);
//        return ResponseEntity.ok()
//            .lastModified(maxTs.toEpochMilli())
//            .body(body);
//    }

    @Override
    protected JpaIdentifiableRepository<T> getRepository() {
        return (JpaIdentifiableRepository<T>) super.getRepository();
    }
}
