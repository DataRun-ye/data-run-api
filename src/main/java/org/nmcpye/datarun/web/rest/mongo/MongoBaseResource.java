package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.common.AuditableObjectRepository;
import org.nmcpye.datarun.web.rest.common.BaseReadWriteResource;

//@RequestMapping("ApiVersion.API_CUSTOM, ApiVersion.API_V1")
public abstract class MongoBaseResource<T extends AuditableObject<String>>
    extends BaseReadWriteResource<T, String> {

    protected MongoBaseResource(AuditableObjectService<T, String> identifiableService,
                                AuditableObjectRepository<T, String> repository) {
        super(identifiableService, repository);
    }
}
