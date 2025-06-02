package org.nmcpye.datarun.mongo.legacydatatemplate.service;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.datatemplate.service.DataTemplateInstanceService;
import org.nmcpye.datarun.mongo.common.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.mongo.datatemplateversion.mapper.DataFormTemplateMapper;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.legacydatatemplate.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Implementation for managing {@link DataFormTemplate}.
 */
@Service
@Primary
@Slf4j
@Transactional
public class DefaultDataFormTemplateService
    extends DefaultMongoAuditableObjectService<DataFormTemplate>
    implements DataFormTemplateService {
    private final DataFormTemplateMapper dataFormTemplateMapper;
    private final DataTemplateInstanceService templateInstanceService;

    public DefaultDataFormTemplateService(
        DataFormTemplateRepository repository,
        CacheManager cacheManager,
        DataFormTemplateMapper dataFormTemplateMapper,
        DataTemplateInstanceService templateInstanceService) {
        super(repository, cacheManager);
        this.dataFormTemplateMapper = dataFormTemplateMapper;
        this.templateInstanceService = templateInstanceService;
    }

    @Override
    public DataFormTemplate saveWithRelations(DataFormTemplate object) {
        log.debug("Request service to save {}:`{}`", getClazz().getSimpleName(), object.getUid());
        return save(object);
    }

    @Override
    public Page<DataFormTemplate> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        return templateInstanceService.findAllByUser(queryRequest, jsonQueryBody)
            .map(dataFormTemplateMapper::toEntity);
    }

    @Override
    public boolean existsByUid(String uid) {
        return templateInstanceService.findByUid(uid).isPresent();
    }

    @Override
    public Optional<DataFormTemplate> findByUid(String uid) {
        return templateInstanceService
            .findByUid(uid).map(dataFormTemplateMapper::toEntity);
    }

    @Override
    public void deleteByUid(String uid) {
        templateInstanceService.deleteByUid(uid);
    }

    @Override
    public DataFormTemplate save(DataFormTemplate object) {
        log.debug("Request service to save {}:`{}`", getClazz().getSimpleName(), object.getUid());
        return dataFormTemplateMapper
            .toEntity(templateInstanceService.save(dataFormTemplateMapper.toDto(object)));
    }

    @Override
    public void delete(DataFormTemplate object) {
        findByUid(object.getUid()).ifPresent(repository::delete);
    }

    @Transactional
    public DataFormTemplate update(DataFormTemplate object) {
        log.debug("Request service to update {}:`{}`", getClazz().getSimpleName(), object.getUid());
        return dataFormTemplateMapper.toEntity(
            templateInstanceService.update(dataFormTemplateMapper.toDto(object)));
    }
}
