package org.nmcpye.datarun.jpa.datatemplate.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link DataTemplate}.
 */
@Service
@Transactional
public class DefaultDataTemplateService
    extends DefaultJpaSoftDeleteService<DataTemplate>
    implements DataTemplateService {

    public DefaultDataTemplateService(DataTemplateRepository repository,
                                      CacheManager cacheManager,
                                      UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
    }
}
