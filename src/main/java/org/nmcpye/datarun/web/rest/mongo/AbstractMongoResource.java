package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.common.repository.AuditableObjectRepository;
import org.nmcpye.datarun.web.rest.common.AbstractResourceReadWrite;

//@RequestMapping("/api/custom")
public abstract class AbstractMongoResource<T extends AuditableObject<String>>
    extends AbstractResourceReadWrite<T, String> {
    protected AbstractMongoResource(AuditableObjectService<T, String> identifiableService, AuditableObjectRepository<T, String> repository) {
        super(identifiableService, repository);
    }
}
