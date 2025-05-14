package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.common.mongo.impl.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.mapper.FormTemplateMapper;
import org.nmcpye.datarun.mapper.FormTemplateVersionMapper;
import org.nmcpye.datarun.mapper.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
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
import org.springframework.data.mongodb.core.MongoTemplate;
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
public class FormTemplateVersionServiceImpl
    extends DefaultMongoAuditableObjectService<FormTemplateVersion>
    implements FormTemplateVersionService {
    private final FormTemplateRepository templateRepository;
    private final FormTemplateVersionRepository templateVersionRepository;
    private final VersionSequenceService seqSvc;
    private final FormTemplateMapper templateMapper;
    private final FormTemplateVersionMapper versionMapper;
    protected final MongoQueryBuilder mongoQueryBuilder;
    protected final GenericQueryService queryService;

    public FormTemplateVersionServiceImpl(
        FormTemplateVersionRepository repository,
        CacheManager cacheManager,
        MongoTemplate mongoTemplate,
        FormTemplateRepository templateRepository,
        FormTemplateVersionRepository templateVersionRepository,
        VersionSequenceService seqSvc,
        FormTemplateMapper templateMapper,
        FormTemplateVersionMapper versionMapper,
        MongoQueryBuilder mongoQueryBuilder, GenericQueryService queryService) {
        super(repository, cacheManager, mongoTemplate);
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.seqSvc = seqSvc;
        this.templateMapper = templateMapper;
        this.versionMapper = versionMapper;
        this.mongoQueryBuilder = mongoQueryBuilder;
        this.queryService = queryService;
    }

    @Transactional
    public FormTemplateVersionDto saveNewVersion(SaveFormTemplateDto saveFormTemplateDto) {
        final var templateUid = saveFormTemplateDto.getUid();
        FormTemplate ver = seqSvc.incrementAndGet(templateUid);
        String versionedId = saveFormTemplateDto.getUid() + "_" + ver.getLatestVersion();

        saveFormTemplateDto.setVersion(ver.getLatestVersion());
        // persist the payload
        // 2) persist the payload
        FormTemplateVersion ftv = versionMapper.fromSaveDto(saveFormTemplateDto);
        ftv.setId(versionedId);
        ftv.setUid(versionedId);
        ftv.setTemplateUid(templateUid);

        return versionMapper.toDto(templateVersionRepository.save(ftv));
    }

    @Override
    public Page<FormTemplateVersionDto> findAllLatest(QueryRequest queryRequest, String jsonQueryBody) {
        // load only lightweight masters
        Page<FormTemplate> masters = getMasterList(queryRequest, jsonQueryBody);
        // batch-load versions
        List<String> ids = masters.stream()
            .map(m -> m.getUid() + "_" + m.getLatestVersion())
            .toList();

        Map<String, FormTemplateVersion> versions = templateVersionRepository.findAllById(ids).stream()
            .collect(Collectors.toMap(FormTemplateVersion::getTemplateUid, s -> s));

        return masters.map(m -> templateMapper.combineMasterAndVersion(m,
            Optional.ofNullable(versions.get(m.getUid())).orElseThrow()));
    }

    public FormTemplateVersionDto findByVersion(String masterUid, int version) {
        String id = masterUid + "_" + version;
        FormTemplate master = templateRepository.findById(masterUid)
            .orElseThrow(() -> new IllegalQueryException(ErrorCode.E1113, masterUid));
        FormTemplateVersionDto versionDto = templateVersionRepository.findById(id).map(versionMapper::toDto)
            .orElseThrow(() -> new IllegalQueryException(ErrorCode.E1114, id));
        return versionDto;
    }


    public Page<FormTemplateVersionDto> pageVersions(String templateId, Pageable pageable) {
        Page<FormTemplateVersion> page = templateVersionRepository.findAllByTemplateUidOrderByVersionDesc(templateId, pageable);
        return page.map(versionMapper::toDto);
    }

    private void applySecurityConstraints(Query query) {
        if (SecurityUtils.isSuper()) {
            return;
        }

        final CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();

        query.addCriteria(Criteria.where("uid").in(user.getUserFormsUIDs()));
    }

    private Page<FormTemplate> getMasterList(QueryRequest queryRequest, String jsonQueryBody) {
        final Query query = new Query();
        if (!SecurityUtils.isSuper()) {
            applySecurityConstraints(query);
        }

        if (!queryRequest.isIncludeDeleted()) {
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
    public FormTemplateVersionDto findLatestByTemplate(String templateUid) {
        return templateVersionRepository.findTopByTemplateUidOrderByVersionDesc(templateUid)
            .map(versionMapper::toDto)
            .orElseThrow(() -> new IllegalQueryException(
                new ErrorMessage(ErrorCode.E1004,
                    getClazz().getSimpleName(), templateUid)));
    }
}
