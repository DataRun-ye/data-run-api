package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.common.repository.UserRepository;
import org.nmcpye.datarun.mapper.FormTemplateVersionMapper;
import org.nmcpye.datarun.mapper.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplate;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplateVersion;
import org.nmcpye.datarun.mongo.repository.FormTemplateRepository;
import org.nmcpye.datarun.mongo.service.FormTemplateService;
import org.nmcpye.datarun.mongo.service.FormTemplateVersionService;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Implementation for managing {@link FormTemplate}.
 */
@Service
@Primary
@Transactional
public class FormTemplateServiceImpl implements FormTemplateService {
    private final FormTemplateVersionService templateVersionService;
    private final FormTemplateRepository templateRepository;
    private final FormTemplateVersionMapper versionMapper;

    public FormTemplateServiceImpl(FormTemplateVersionService templateVersionService,
                                   FormTemplateRepository templateRepository, FormTemplateVersionMapper versionMapper) {
        this.templateVersionService = templateVersionService;
        this.templateRepository = templateRepository;
        this.versionMapper = versionMapper;
    }

    @Override
    public boolean existsByUid(String uid) {
        return templateVersionService.findLatestByTemplate(uid).isPresent();
    }

    @Override
    public Optional<FormTemplateVersionDto> findByUid(String uid) {
        return templateVersionService.findLatestByTemplate(uid);
    }

    @Override
    public void deleteByUid(String uid) {
        templateRepository.findByUid(uid).ifPresent((t) -> {
            t.setDeleted(true);
            templateRepository.save(t);
        });
    }

    @Override
    public Page<FormTemplateVersionDto> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        return templateVersionService.findAllLatest(queryRequest, jsonQueryBody);
    }

    @CacheEvict(cacheNames = {UserRepository.USER_TEAM_FORM_ACCESS_CACHE,
        UserRepository.USER_ACTIVITY_IDS_CACHE,
        UserRepository.USER_TEAM_IDS_CACHE})
    @Override
    public FormTemplateVersionDto save(SaveFormTemplateDto saveFormTemplateDto) {
        return templateVersionService.saveNewVersion(saveFormTemplateDto);
    }

    @Override
    public FormTemplateVersionDto update(SaveFormTemplateDto saveFormTemplateDto) {
        templateVersionService
            .findLatestByTemplate(saveFormTemplateDto.getUid()).orElseThrow(() -> new IllegalQueryException(
                new ErrorMessage(ErrorCode.E1004,
                    FormTemplateVersion.class.getSimpleName(), saveFormTemplateDto.getUid())));
        return save(saveFormTemplateDto);
    }

    @Override
    public void delete(SaveFormTemplateDto object) {
        deleteByUid(object.getUid());
    }
}
