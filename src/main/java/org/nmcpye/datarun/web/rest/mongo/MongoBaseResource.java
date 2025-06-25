package org.nmcpye.datarun.web.rest.mongo;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.IdentifiableObjectService;
import org.nmcpye.datarun.mongo.common.MongoIdentifiableObject;
import org.nmcpye.datarun.mongo.common.repository.MongoIdentifiableRepository;
import org.nmcpye.datarun.web.rest.common.BaseReadWriteResource;

//@RequestMapping("ApiVersion.API_CUSTOM, ApiVersion.API_V1")
@Slf4j
public abstract class MongoBaseResource<T extends MongoIdentifiableObject>
    extends BaseReadWriteResource<T, String> {

    protected MongoBaseResource(IdentifiableObjectService<T, String> identifiableService,
                                MongoIdentifiableRepository<T> repository) {
        super(identifiableService, repository);
    }
}
