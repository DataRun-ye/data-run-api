package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.common.IdentifiableRepository;
import org.nmcpye.datarun.common.IdentifiableService;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableEntity;
import org.nmcpye.datarun.web.rest.common.AbstractResourceReadWrite;

//@RequestMapping("/api/custom")
public abstract class AbstractMongoResource<T extends IdentifiableEntity<String>>
    extends AbstractResourceReadWrite<T, String> {
    protected AbstractMongoResource(IdentifiableService<T, String> identifiableService, IdentifiableRepository<T, String> repository) {
        super(identifiableService, repository);
    }
}
