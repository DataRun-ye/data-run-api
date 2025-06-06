package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.common.IdentifiableObjectRepository;
import org.nmcpye.datarun.common.IdentifiableObjectService;
import org.nmcpye.datarun.mongo.common.MongoIdentifiableObject;
import org.nmcpye.datarun.web.rest.common.BaseReadWriteResource;

//@RequestMapping("ApiVersion.API_CUSTOM, ApiVersion.API_V1")
public abstract class MongoBaseResource<T extends MongoIdentifiableObject>
    extends BaseReadWriteResource<T, String> {

    protected MongoBaseResource(IdentifiableObjectService<T, String> identifiableService,
                                IdentifiableObjectRepository<T, String> repository) {
        super(identifiableService, repository);
    }
}
