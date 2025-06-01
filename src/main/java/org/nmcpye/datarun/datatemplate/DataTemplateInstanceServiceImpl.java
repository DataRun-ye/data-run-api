package org.nmcpye.datarun.datatemplate;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.datatemplateversion.DataTemplateTemplateVersion;
import org.nmcpye.datarun.datatemplateversion.FormTemplateVersionService;
import org.nmcpye.datarun.datatemplateversion.mapper.FormTemplateVersionMapper;
import org.nmcpye.datarun.user.repository.UserRepository;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Implementation for managing {@link DataTemplate}.
 */
@Service
@Primary
@Transactional
public class DataTemplateInstanceServiceImpl
    implements DataTemplateInstanceService {
    private final FormTemplateVersionService templateVersionService;
    private final DataTemplateRepository templateRepository;
    private final FormTemplateVersionMapper versionMapper;

    public DataTemplateInstanceServiceImpl(FormTemplateVersionService templateVersionService,
                                           DataTemplateRepository templateRepository,
                                           FormTemplateVersionMapper versionMapper) {
        this.templateVersionService = templateVersionService;
        this.templateRepository = templateRepository;
        this.versionMapper = versionMapper;
    }

    @Override
    public boolean existsByUid(String uid) {
        return templateVersionService.findLatestByTemplate(uid).isPresent();
    }

    @Override
    public Optional<DataTemplateInstanceDto> findByUid(String uid) {
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
    public Page<DataTemplateInstanceDto> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        return templateVersionService.findAllLatest(queryRequest, jsonQueryBody);
    }

    @CacheEvict(cacheNames = {UserRepository.USER_TEAM_FORM_ACCESS_CACHE,
        UserRepository.USER_ACTIVITY_IDS_CACHE,
        UserRepository.USER_TEAM_IDS_CACHE})
    @Override
    public DataTemplateInstanceDto save(DataTemplateInstanceDto dataTemplateInstanceDto) {
        return templateVersionService.saveNewVersion(dataTemplateInstanceDto);
    }

    @Override
    public DataTemplateInstanceDto update(DataTemplateInstanceDto dataTemplateInstanceDto) {
        templateVersionService
            .findLatestByTemplate(dataTemplateInstanceDto.getUid()).orElseThrow(() -> new IllegalQueryException(
                new ErrorMessage(ErrorCode.E1004,
                    DataTemplateTemplateVersion.class.getSimpleName(), dataTemplateInstanceDto.getUid())));
        return save(dataTemplateInstanceDto);
    }

    @Override
    public void delete(DataTemplateInstanceDto object) {
        deleteByUid(object.getUid());
    }
}
