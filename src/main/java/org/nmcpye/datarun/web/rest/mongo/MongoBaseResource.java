package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.common.repository.AuditableObjectRepository;
import org.nmcpye.datarun.query.MongoQueryBuilder;
import org.nmcpye.datarun.web.rest.common.BaseReadWriteResource;
import org.springframework.beans.factory.annotation.Autowired;

//@RequestMapping("/api/custom")
public abstract class MongoBaseResource<T extends AuditableObject<String>>
    extends BaseReadWriteResource<T, String> {

    @Autowired
    private MongoQueryBuilder mongoQueryBuilder;

    protected MongoBaseResource(AuditableObjectService<T, String> identifiableService,
                                AuditableObjectRepository<T, String> repository) {
        super(identifiableService, repository);
    }

    @Override
    protected MongoQueryBuilder getQueryBuilder() {
        return mongoQueryBuilder;
    }
}
