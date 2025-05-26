package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.mongo.impl.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.common.repository.AuditableObjectRepository;
import org.nmcpye.datarun.mapper.DataFormTemplateMapper;
import org.nmcpye.datarun.mapper.FormTemplateMapper;
import org.nmcpye.datarun.mapper.FormTemplateVersionMapper;
import org.nmcpye.datarun.mapper.SaveDataFormTemplateMapper;
import org.nmcpye.datarun.mapper.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplate;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplateVersion;
import org.nmcpye.datarun.mongo.repository.FormTemplateRepository;
import org.nmcpye.datarun.mongo.repository.FormTemplateVersionRepository;
import org.nmcpye.datarun.mongo.service.FormTemplateVersionService;
import org.nmcpye.datarun.mongo.service.VersionSequenceService;
import org.nmcpye.datarun.query.MongoQueryBuilder;
import org.nmcpye.datarun.query.UnifiedQueryParser;
import org.nmcpye.datarun.query.filter.FilterExpression;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.GenericQueryService;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
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
 * Service Implementation for managing {@link FormTemplateVersion}.
 */
@Service
@Primary
@Transactional
public class DefaultFormTemplateVersionService
    extends DefaultMongoAuditableObjectService<FormTemplateVersion>
    implements FormTemplateVersionService {
    private final FormTemplateVersionRepository templateVersionRepository;
    private final FormTemplateRepository templateRepository;
    private final VersionSequenceService seqSvc;
    protected final MongoQueryBuilder mongoQueryBuilder;
    protected final GenericQueryService queryService;
    private final FormTemplateVersionMapper versionMapper;
    private final FormTemplateMapper formTemplateMapper;
    private final SaveDataFormTemplateMapper saveTemplateMapper;
    private final DataFormTemplateMapper dataFormTemplateMapper;

    public DefaultFormTemplateVersionService(AuditableObjectRepository<FormTemplateVersion, String> repository, CacheManager cacheManager, FormTemplateVersionRepository templateVersionRepository, FormTemplateRepository templateRepository, VersionSequenceService seqSvc, MongoQueryBuilder mongoQueryBuilder, GenericQueryService queryService, FormTemplateVersionMapper versionMapper, FormTemplateMapper formTemplateMapper, SaveDataFormTemplateMapper saveTemplateMapper, DataFormTemplateMapper dataFormTemplateMapper) {
        super(repository, cacheManager);
        this.templateVersionRepository = templateVersionRepository;
        this.templateRepository = templateRepository;
        this.seqSvc = seqSvc;
        this.mongoQueryBuilder = mongoQueryBuilder;
        this.queryService = queryService;
        this.versionMapper = versionMapper;
        this.formTemplateMapper = formTemplateMapper;
        this.saveTemplateMapper = saveTemplateMapper;
        this.dataFormTemplateMapper = dataFormTemplateMapper;
    }


    /**
     * pumping form template's version, and update/adding formTemplateVersion with the new details and persist into db
     *
     * @param saveFormTemplateDto incoming merged template, can be new (uid = null), or update
     * @return SaveFormTemplateDto with current updated details merged from both
     */
    @Transactional
    @Override
    public SaveFormTemplateDto saveNewVersion(SaveFormTemplateDto saveFormTemplateDto) {
        final FormTemplate formTemplate = formTemplateMapper.fromSaveDto(saveFormTemplateDto);
        // pumping version or adding new and update
        final FormTemplate ver = seqSvc.incrementAndGet(formTemplate.getUid());
        // setting form template details and save
        final FormTemplateVersion templateVersion = templateVersionRepository.save(versionMapper
            .fromSaveDto(saveFormTemplateDto)
            .version(ver.getVersionNumber())
            .templateUid(formTemplate.getUid()));

        templateRepository.save(formTemplate.versionNumber(ver.getVersionNumber())
            // temporary for migrating old DataFormTemplate
            .id(ver.getId())
            .formVersion(templateVersion.getUid()));


        // returning SaveFormTemplateDto with current updated details merged from both
        return saveTemplateMapper.combineMasterAndVersion(formTemplate, templateVersion);
    }

    @Transactional
    @Override
    public void migrateDataFormTemplateVersion(DataFormTemplate formTemplate) {
        if (!templateRepository.existsByUid(formTemplate.getUid())) {
            final var formTemplateVersion = formTemplate.getVersion();
            final var templateUid = formTemplate.getUid();

            final SaveFormTemplateDto saveFormTemplateDto = dataFormTemplateMapper.toDto(formTemplate);
            final FormTemplate template = formTemplateMapper.fromSaveDto(saveFormTemplateDto);

            final FormTemplateVersion templateVersion = templateVersionRepository.save(versionMapper
                .fromSaveDto(saveFormTemplateDto)
                .version(formTemplateVersion)
                .templateUid(templateUid));

            templateRepository.save(template.versionNumber(formTemplateVersion)
                // temporary for migrating old DataFormTemplate
                .id(formTemplate.getId())
                .uid(templateUid)
                .formVersion(templateVersion.getUid()));
        }
    }

    @Override
    public Page<FormTemplateVersion> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        if (!SecurityUtils.isSuper()) {
            final CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();
            final var paging = queryRequest.getPageableMongo();
            return templateVersionRepository.findTopByTemplateUidInOrderByVersionNumberDesc(user.getUserFormsUIDs(),
                paging);
        }

        return super.findAllByUser(queryRequest, jsonQueryBody);
    }

    @Override
    public Page<SaveFormTemplateDto> findAllLatest(QueryRequest queryRequest, String jsonQueryBody) {
        // load only lightweight masters
        Page<FormTemplate> masters = getMasterList(queryRequest, jsonQueryBody);
        // batch-load versions
        List<String> ids = masters.stream()
            .map(FormTemplate::getFormVersion)
            .toList();

        Map<String, FormTemplateVersion> versions = templateVersionRepository.findAllByUidIn(ids).stream()
            .collect(Collectors.toMap(FormTemplateVersion::getTemplateUid, s -> s));

        return masters.map(m -> saveTemplateMapper.combineMasterAndVersion(m,
            Optional.ofNullable(versions.get(m.getUid())).orElseThrow()));
    }

    public FormTemplateVersionDto findByVersion(String masterUid, int version) {
        FormTemplateVersionDto versionDto = templateVersionRepository.findByTemplateUidAndVersionNumber(masterUid, version).map(versionMapper::toDto)
            .orElseThrow(() -> new IllegalQueryException(ErrorCode.E1114, masterUid + ":" + version));
        return versionDto;
    }

    public Page<FormTemplateVersionDto> pageVersions(String templateId, Pageable pageable) {
        Page<FormTemplateVersion> page = templateVersionRepository.findAllByTemplateUidOrderByVersionNumberDesc(templateId, pageable);
        return page.map(versionMapper::toDto);
    }

    public void applySecurityConstraints(Query query) {
        if (SecurityUtils.isSuper()) {
            return;
        }

        final CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();

        query.addCriteria(Criteria.where("templateUid").in(user.getUserFormsUIDs()));
    }

    private Page<FormTemplate> getMasterList(QueryRequest queryRequest, String jsonQueryBody) {
        final Query query = new Query();
        if (!SecurityUtils.isSuper()) {
            final CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();
            query.addCriteria(Criteria.where("uid").in(user.getUserFormsUIDs()));
        }

        if (queryRequest == null || !queryRequest.isIncludeDeleted()) {
            query.addCriteria(Criteria.where("deleted").is(false));
        }

        if (jsonQueryBody != null && !jsonQueryBody.isEmpty()) {
            try {
                FilterExpression filterExpression = UnifiedQueryParser.parse(jsonQueryBody);
                query.addCriteria(mongoQueryBuilder.buildCriteria(List.of(filterExpression)));
            } catch (Exception e) {
                throw new IllegalQueryException(ErrorCode.E2050, jsonQueryBody);
            }
        }
        return queryService.query(queryRequest, query, FormTemplate.class);
    }

    @Override
    public Optional<SaveFormTemplateDto> findLatestByTemplate(String templateUid) {
        final var template = templateRepository.findByUid(templateUid);
        final var version = templateVersionRepository.findTopByTemplateUidOrderByVersionNumberDesc(templateUid);
        if (template.isEmpty() || version.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(saveTemplateMapper.combineMasterAndVersion(template.get(), version.get()));
    }
}
