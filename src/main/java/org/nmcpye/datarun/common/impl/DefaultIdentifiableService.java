package org.nmcpye.datarun.common.impl;

import org.nmcpye.datarun.common.IdentifiableObject;
import org.nmcpye.datarun.common.repository.IdentifiableRepository;
import org.nmcpye.datarun.common.IdentifiableService;
import org.springframework.cache.CacheManager;

/**
 * @author Hamza, 20/03/2025
 */
public class DefaultIdentifiableService<T extends IdentifiableObject<ID>, ID>
    extends DefaultAuditableObjectService<T, ID>
    implements IdentifiableService<T, ID> {

    public DefaultIdentifiableService(IdentifiableRepository<T, ID> repository, CacheManager cacheManager) {
        super(repository, cacheManager);
    }
}
