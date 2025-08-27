package org.nmcpye.datarun.mongo.datatemplateversion.service;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.mapper.DataTemplateMapper;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.datatemplate.service.DataTemplateService;
import org.nmcpye.datarun.mongo.common.DefaultMongoIdentifiableObjectService;
import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion;
import org.nmcpye.datarun.mongo.datatemplateversion.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.mongo.datatemplateversion.mapper.FormTemplateVersionMapper;
import org.nmcpye.datarun.mongo.datatemplateversion.repository.DataTemplateVersionRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link DataTemplateVersion}.
 */
@Service
@Primary
@Transactional
@Slf4j
@SuppressWarnings("unused")
public class DefaultDataTemplateVersionService
    extends DefaultMongoIdentifiableObjectService<DataTemplateVersion>
    implements DateTemplateVersionService {
    private final DataTemplateService dataTemplateService;
    private final DataTemplateVersionRepository templateVersionRepository;
    private final FormTemplateVersionMapper versionMapper;

    private final DataTemplateRepository templateRepository;
    private final DataTemplateMapper dataTemplateMapper;

    public DefaultDataTemplateVersionService(DataTemplateVersionRepository repository,
                                             CacheManager cacheManager, DataTemplateService dataTemplateService,
                                             FormTemplateVersionMapper versionMapper,
                                             DataTemplateRepository templateRepository,
                                             @Lazy DataTemplateMapper dataTemplateMapper) {
        super(repository, cacheManager);
        this.templateVersionRepository = repository;
        this.dataTemplateService = dataTemplateService;
        this.versionMapper = versionMapper;
        this.templateRepository = templateRepository;
        this.dataTemplateMapper = dataTemplateMapper;
    }

//    private FormTemplate pumpTemplateVersion(FormTemplate formTemplate) {
//        return templateRepository.findByUidForWrite(formTemplate.getUid())
//            // just increases current version number
//            .map(FormTemplate::pumpVersion)
//            .orElse(formTemplate.versionNumber(1));
//    }
//
//    /**
//     * pumping form template's version, and update/adding formTemplateVersion with the new details and persist into db
//     *
//     * @param formTemplateInstanceDto incoming merged template, can be new (id = null), or update
//     * @return SaveFormTemplateDto with current updated details merged from both
//     */
//    @Transactional
//    @Override
//    public FormTemplateInstanceDto saveNewVersion(FormTemplateInstanceDto formTemplateInstanceDto) {
//        final FormTemplate formTemplate = formTemplateMapper.fromInstanceDto(formTemplateInstanceDto);
//        // pumping version or adding new and update
//        final FormTemplate ver = pumpTemplateVersion(formTemplate);
//        // setting form template details and save
//
//        final FormTemplateVersion templateVersion = templateVersionRepository.save(versionMapper
//            .fromInstanceDto(formTemplateInstanceDto)
//            // pumped version
//            .version(ver.getVersionNumber())
//            // formTemplate Uid of version
//            .templateUid(formTemplate.getUid()));
//
//        templateRepository.save(formTemplate.versionNumber(ver.getVersionNumber())
//            // update formVersionUid
//            .formVersion(templateVersion.getUid()));
//
//
//        // returning instance with current updated details merged from both
//        return formTemplateMapper.toInstanceDto(formTemplateMapper.toDto(formTemplate),
//            versionMapper.toDto(templateVersion));
//    }

//    @Transactional
//    @Override
//    public void migrateDataFormTemplateVersionToLegacy(DataFormTemplate formTemplate) {
//        if (!templateRepository.existsByUid(formTemplate.getUid())) {
//            final var formTemplateVersion = formTemplate.getVersion();
//            final var templateUid = formTemplate.getUid();
//
//            final FormTemplateInstanceDto formInstanceDto = dataFormTemplateMapper.toDto(formTemplate);
//            final FormTemplateLegacy template = formTemplateLegacyMapper.fromInstanceDto(formInstanceDto);
//
//            final FormTemplateVersion templateVersion = templateVersionRepository.save(versionMapper
//                .fromInstanceDto(formInstanceDto)
//                .version(formTemplateVersion)
//                .templateUid(templateUid));
//
//            templateLegacyRepository.save(template.versionNumber(formTemplateVersion)
//                // temporary for migrating old DataFormTemplate
//                .id(formTemplate.getId())
//                .id(templateUid)
//                .formVersion(templateVersion.getUid()));
//        }
//    }

    @Override
    public Page<DataTemplateVersion> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        // load only lightweight masters
        Page<DataTemplate> masters = dataTemplateService.findAllByUser(queryRequest, jsonQueryBody);
        // batch-load versions
        List<String> ids = masters.stream()
            .map(DataTemplate::getVersionUid)
            .toList();

        Map<String, DataTemplateVersion> versions = templateVersionRepository.findAllByUidIn(ids).stream()
            .collect(Collectors.toMap(DataTemplateVersion::getTemplateUid, s -> s));

        return masters.map(m -> versions.get(m.getUid()));
    }

    @Override
    public Optional<DataTemplateVersion> findLatestByTemplate(String templateUid) {
        return templateVersionRepository.findTopByTemplateUidOrderByVersionNumberDesc(templateUid);
    }

    @Override
    public FormTemplateVersionDto findByVersion(String masterUid, int version) {
        return templateVersionRepository.findByTemplateUidAndVersionNumber(masterUid, version).map(versionMapper::toDto)
            .orElseThrow(() -> new IllegalQueryException(ErrorCode.E1114, masterUid + ":" + version));
    }

    @Override
    public Page<FormTemplateVersionDto> pageVersions(String templateId, Pageable pageable) {
        Page<DataTemplateVersion> page = templateVersionRepository.findAllByTemplateUidOrderByVersionNumberDesc(templateId, pageable);
        return page.map(versionMapper::toDto);
    }

    @Override
    public void applySecurityConstraints(Query query) {
        if (SecurityUtils.isSuper()) {
            return;
        }

        final CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();

        query.addCriteria(Criteria.where("templateUid").in(user.getUserFormsUIDs()));
    }
}
