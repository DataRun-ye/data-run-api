package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.common.IdentifiableRepository;
import org.nmcpye.datarun.drun.postgres.common.Identifiable;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableService;
import org.nmcpye.datarun.web.rest.common.AbstractResourceReadWrite;

//@RequestMapping("/api/custom")
public abstract class AbstractMongoResource<T extends Identifiable<String>>
    extends AbstractResourceReadWrite<T, String> {
    protected AbstractMongoResource(IdentifiableService<T, String> identifiableService, IdentifiableRepository<T, String> repository) {
        super(identifiableService, repository);
    }
}
