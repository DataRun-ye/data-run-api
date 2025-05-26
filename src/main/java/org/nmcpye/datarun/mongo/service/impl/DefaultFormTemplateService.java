package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.common.mongo.impl.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.mapper.FormTemplateVersionMapper;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplate;
import org.nmcpye.datarun.mongo.repository.FormTemplateRepository;
import org.nmcpye.datarun.mongo.service.FormTemplateService;
import org.nmcpye.datarun.mongo.service.FormTemplateVersionService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link FormTemplate}.
 */
@Service
@Primary
@Transactional
public class DefaultFormTemplateService
    extends DefaultMongoAuditableObjectService<FormTemplate>
    implements FormTemplateService {
    private final FormTemplateVersionService templateVersionService;
    private final FormTemplateRepository templateRepository;
    private final FormTemplateVersionMapper versionMapper;

    public DefaultFormTemplateService(CacheManager cacheManager,
                                      FormTemplateVersionService templateVersionService,
                                      FormTemplateRepository templateRepository,
                                      FormTemplateVersionMapper versionMapper) {
        super(templateRepository, cacheManager);
        this.templateVersionService = templateVersionService;
        this.templateRepository = templateRepository;
        this.versionMapper = versionMapper;
    }

    public void applySecurityConstraints(Query query) {
        if (SecurityUtils.isSuper()) {
            return;
        }

        final CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();

        query.addCriteria(Criteria.where("uid").in(user.getUserFormsUIDs()));
    }
}
