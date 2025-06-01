package org.nmcpye.datarun.datatemplate;

import org.nmcpye.datarun.common.jpa.DefaultJpaSoftDeleteService;
import org.nmcpye.datarun.common.jpa.repository.JpaAuditableRepository;
import org.nmcpye.datarun.security.useraccess.UserAccessService;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link DataTemplate}.
 */
@Service
@Primary
@Transactional
public class DefaultDataTemplateService
    extends DefaultJpaSoftDeleteService<DataTemplate>
    implements DataTemplateService {

    public DefaultDataTemplateService(JpaAuditableRepository<DataTemplate> jpaAuditableObjectRepository, CacheManager cacheManager, UserAccessService userAccessService, UserAccessService userAccessService1, JpaAuditableRepository<DataTemplate> jpaAuditableObjectRepository1) {
        super(jpaAuditableObjectRepository, cacheManager, userAccessService, userAccessService1, jpaAuditableObjectRepository1);
    }



}
