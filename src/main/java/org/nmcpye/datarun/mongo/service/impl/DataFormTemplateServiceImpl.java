package org.nmcpye.datarun.mongo.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.mongo.impl.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.mapper.DataFormTemplateMapper;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.service.DataFormTemplateService;
import org.nmcpye.datarun.mongo.service.FormTemplateService;
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
public class DataFormTemplateServiceImpl
    extends DefaultMongoAuditableObjectService<DataFormTemplate>
    implements DataFormTemplateService {
    private final DataFormTemplateMapper dataFormTemplateMapper;
    private final FormTemplateService templateService;

    public DataFormTemplateServiceImpl(
        DataFormTemplateRepository repository,
        CacheManager cacheManager,
        DataFormTemplateMapper dataFormTemplateMapper,
        FormTemplateService templateService) {
        super(repository, cacheManager);
        this.dataFormTemplateMapper = dataFormTemplateMapper;
        this.templateService = templateService;
    }

    @Override
    public DataFormTemplate saveWithRelations(DataFormTemplate object) {
        log.debug("Request service to save {}:`{}`", getClazz().getSimpleName(), object.getUid());
        return save(object);
    }

    @Override
    public Page<DataFormTemplate> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        return templateService.findAllByUser(queryRequest, jsonQueryBody)
            .map(dataFormTemplateMapper::fromVersionDto);
    }

    @Override
    public boolean existsByUid(String uid) {
        return templateService.findByUid(uid).isPresent();
    }

    @Override
    public Optional<DataFormTemplate> findByUid(String uid) {
        return templateService
            .findByUid(uid)
            .map(dataFormTemplateMapper::fromVersionDto);
    }

    @Override
    public void deleteByUid(String uid) {
        templateService.deleteByUid(uid);
    }

    @Override
    public DataFormTemplate save(DataFormTemplate object) {
        log.debug("Request service to save {}:`{}`", getClazz().getSimpleName(), object.getUid());
        return dataFormTemplateMapper.fromVersionDto(
            templateService.save(
                dataFormTemplateMapper.toDto(object)));
    }

    @Override
    public Optional<DataFormTemplate> findByIdentifyingProperties(DataFormTemplate identifiableObject) {
        return templateService
            .findByUid(identifiableObject.getUid())
            .map(dataFormTemplateMapper::fromVersionDto);
    }

    @Override
    public void delete(DataFormTemplate object) {
        findByIdentifyingProperties(object).ifPresent(repository::delete);
    }

    @Transactional
    public DataFormTemplate update(DataFormTemplate object) {
        log.debug("Request service to update {}:`{}`", getClazz().getSimpleName(), object.getUid());

        return dataFormTemplateMapper
            .fromVersionDto(templateService.update(dataFormTemplateMapper.toDto(object)));
    }
}
