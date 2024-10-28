package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.drun.common.IdentifiableRepository;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableService;
import org.nmcpye.datarun.web.rest.common.AbstractResource;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/custom")
public abstract class AbstractMongoResource<T extends IdentifiableObject<String>>
    extends AbstractResource<T, String> {
    protected AbstractMongoResource(IdentifiableService<T, String> identifiableService, IdentifiableRepository<T, String> repository) {
        super(identifiableService, repository);
    }

//    protected AbstractMongoResource(IdentifiableMongoService<T> identifiableService,
//                                    MongoRepository<T, String> repository) {
//        super(identifiableService, repository);
//    }
}
