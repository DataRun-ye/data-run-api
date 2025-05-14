package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.common.mongo.impl.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.common.repository.UserRepository;
import org.nmcpye.datarun.mapper.FormTemplateMapper;
import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplate;
import org.nmcpye.datarun.mongo.repository.FormTemplateRepository;
import org.nmcpye.datarun.mongo.service.FormTemplateService;
import org.nmcpye.datarun.mongo.service.FormTemplateVersionService;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link FormTemplate}.
 */
@Service
@Primary
@Transactional
public class FormTemplateServiceImpl
    extends DefaultMongoAuditableObjectService<FormTemplate>
    implements FormTemplateService {
    private final FormTemplateVersionService templateVersionService;
    private final FormTemplateMapper templateMapper;

    public FormTemplateServiceImpl(
        FormTemplateRepository repository,
        CacheManager cacheManager,
        MongoTemplate mongoTemplate,
        FormTemplateVersionService templateVersionService,
        FormTemplateMapper templateMapper) {
        super(repository, cacheManager, mongoTemplate);
        this.templateVersionService = templateVersionService;
        this.templateMapper = templateMapper;
    }

    @CacheEvict(cacheNames = {UserRepository.USER_TEAM_FORM_ACCESS_CACHE,
        UserRepository.USER_ACTIVITY_IDS_CACHE,
        UserRepository.USER_TEAM_IDS_CACHE})
    public FormTemplate saveWithRelations(SaveFormTemplateDto saveFormTemplateDto) {
        final var ver = templateVersionService.saveNewVersion(saveFormTemplateDto);
        return templateMapper.fromSaveDto(saveFormTemplateDto);
    }
}
