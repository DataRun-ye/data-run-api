package org.nmcpye.datarun.datatemplateversion;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.mongo.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.datatemplate.DataTemplate;
import org.nmcpye.datarun.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.datatemplate.mapper.DataTemplateMapper;
import org.nmcpye.datarun.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.datatemplateversion.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.datatemplateversion.mapper.FormTemplateVersionMapper;
import org.nmcpye.datarun.datatemplateversion.repository.DataTemplateVersionRepository;
import org.nmcpye.datarun.mapper.DataFormTemplateMapper;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplateLegacyRepository;
import org.nmcpye.datarun.query.MongoQueryBuilder;
import org.nmcpye.datarun.query.UnifiedQueryParser;
import org.nmcpye.datarun.query.filter.FilterExpression;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link DataTemplateTemplateVersion}.
 */
@Service
@Primary
@Transactional
@Slf4j
@SuppressWarnings("unused")
public class DefaultFormTemplateVersionService
    extends DefaultMongoAuditableObjectService<DataTemplateTemplateVersion>
    implements FormTemplateVersionService {
    private final DataTemplateVersionRepository templateVersionRepository;
    private final DataTemplateRepository templateRepository;
    private final FormTemplateVersionMapper versionMapper;
    private final DataTemplateMapper dataTemplateMapper;
    private final DataFormTemplateMapper dataFormTemplateMapper;

    public DefaultFormTemplateVersionService(DataTemplateVersionRepository repository,
                                             CacheManager cacheManager,
                                             DataTemplateVersionRepository templateVersionRepository,
                                             DataTemplateRepository templateRepository,
                                             MongoQueryBuilder mongoQueryBuilder,
                                             FormTemplateVersionMapper versionMapper,
                                             DataTemplateMapper dataTemplateMapper,
                                             DataFormTemplateMapper dataFormTemplateMapper,
                                             FormTemplateLegacyRepository templateLegacyRepository) {
        super(repository, cacheManager);
        this.templateVersionRepository = templateVersionRepository;
        this.templateRepository = templateRepository;
        this.versionMapper = versionMapper;
        this.mongoQueryBuilder = mongoQueryBuilder;
        this.dataTemplateMapper = dataTemplateMapper;
        this.dataFormTemplateMapper = dataFormTemplateMapper;
    }

    /**
     * Create a brand‐new version of the FormTemplate (Postgres ↔ Mongo) as a single “unit.”
     * If anything fails, neither side is left half‐updated.
     */
    @Transactional
    public DataTemplateInstanceDto saveNewVersion(DataTemplateInstanceDto dto) {
        // 1) Map incoming DTO → Postgres entity (in memory only, not yet saved).
        DataTemplate maybeNew = dataTemplateMapper.fromInstanceDto(dto);

        // 2) “Lock‐and‐pump” the versionNumber, or initialize to 1 if this is brand‐new.
        //    We call a custom repository method findByUidForWrite(…) that uses a PESSIMISTIC_WRITE lock
        //    so that two concurrent callers cannot both see versionNumber = N and then both flip to N+1.
        DataTemplate template = templateRepository
            .findByUidForWrite(maybeNew.getUid())
            .map(existing -> {
                existing.setVersionNumber(existing.getVersionNumber() + 1);
                return existing;
            })
            .orElseGet(() -> {
                // new template, versionNumber = 1
                maybeNew.setVersionNumber(1);
                return maybeNew;
            });

        int newVersion = template.getVersionNumber();

        // 3) Build the new Mongo “version document” but do NOT touch Postgres yet.
        DataTemplateTemplateVersion versionDoc = versionMapper.fromInstanceDto(dto)
            .version(newVersion)
            .templateUid(template.getUid());

        DataTemplateTemplateVersion savedVersion;
        try {
            savedVersion = templateVersionRepository.save(versionDoc);
            // If this throws (Mongo down, or constraint violation), we immediately bubble up an exception.
            // Because we are @Transactional on Postgres, any Postgres changes (the versionNumber bump) have not been committed yet,
            // so they will roll back automatically. NO need for extra compensation for a Mongo failure.
        } catch (RuntimeException mongoEx) {
            throw new RuntimeException("Failed to save FormTemplateVersion to Mongo", mongoEx);
        }

        // 4) Now that Mongo has given us a new version‐UID, set these two non‐nullable fields on the Postgres entity:
        template.setVersionUid(savedVersion.getUid());
        // (versionNumber is already set, and both versionNumber & formVersionUid are @Column(nullable=false).)

        DataTemplate savedTemplate;
        try {
            savedTemplate = templateRepository.save(template);
            // At this point, we’ve done **two** modifications to Postgres in the same @Transactional:
            //   • either an UPDATE that bumped versionNumber on an existing row (and now sets formVersionUid),
            //   • or an INSERT of a brand‐new row with (uid, versionNumber=1, formVersionUid=<new Mongo UID>).
            //
            // If this save(…) throws (e.g. a unique‐constraint violation, database schema error, etc.), we must delete the Mongo doc.
            // Catch that exception, delete the Mongo version, and rethrow, so that Postgres rolls back its UPDATE/INSERT.
        } catch (RuntimeException postgresEx) {
            // COMPENSATE: remove the Mongo doc we just created, because Postgres did not accept the final save.
            try {
                templateVersionRepository.deleteById(savedVersion.getUid());
            } catch (Exception deleteEx) {
                // If we cannot delete the Mongo doc, log—someone may need to clean it up manually.
                // Either way, we still abort the entire transaction by rethrowing below.
                log.error(
                    "⚠️ Failed to delete orphaned FormTemplateVersion (UID="
                        + savedVersion.getUid()
                        + ") after Postgres save failure.",
                    deleteEx
                );
            }
            // Now rethrow so that the Postgres transaction rolls back its earlier version‐number bump/insert.
            throw new RuntimeException("Failed to save FormTemplate to Postgres", postgresEx);
        }

        // 5) If we reach here, **both** Mongo AND Postgres succeeded. Build the response DTO:
        return dataTemplateMapper.toInstanceDto(
            dataTemplateMapper.toDto(savedTemplate),
            versionMapper.toDto(savedVersion)
        );
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
//     * @param formTemplateInstanceDto incoming merged template, can be new (uid = null), or update
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
//                .uid(templateUid)
//                .formVersion(templateVersion.getUid()));
//        }
//    }

    @Transactional
    @Override
    public void migrateDataFormTemplateVersion(DataFormTemplate formTemplate) {
        if (!templateRepository.existsByUid(formTemplate.getUid())) {
            final var formTemplateVersion = formTemplate.getVersion();
            final var templateUid = formTemplate.getUid();

            final DataTemplateInstanceDto dataTemplateInstanceDto = dataFormTemplateMapper.toDto(formTemplate);
            final DataTemplate template = dataTemplateMapper.fromInstanceDto(dataTemplateInstanceDto);

            final var newVersionUid = CodeGenerator.generateUid();

            templateRepository.save(template.versionNumber(formTemplateVersion)
                // temporary for migrating old DataFormTemplate
                .uid(templateUid)
                .versionNumber(formTemplateVersion)
                .versionUid(newVersionUid));

            final DataTemplateTemplateVersion templateVersion = templateVersionRepository.save(versionMapper
                .fromInstanceDto(dataTemplateInstanceDto)
                .uid(newVersionUid)
                .version(formTemplateVersion)
                .templateUid(templateUid));
        }
    }


    @Override
    public Page<DataTemplateTemplateVersion> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        throw new UnsupportedOperationException("method cannot be used, checkout DefaultFormTemplateVersionService documentation");
//        if (!SecurityUtils.isSuper()) {
//            final CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();
//            final var paging = queryRequest.getPageableMongo();
//            return templateVersionRepository.findTopByTemplateUidInOrderByVersionNumberDesc(user.getUserFormsUIDs(),
//                paging);
//        }
//
//        return super.findAllByUser(queryRequest, jsonQueryBody);
    }

    @Override
    public Page<DataTemplateInstanceDto> findAllLatest(QueryRequest queryRequest, String jsonQueryBody) {
        // load only lightweight masters
        Page<DataTemplate> masters = getMasterList(queryRequest, jsonQueryBody);
        // batch-load versions
        List<String> ids = masters.stream()
            .map(DataTemplate::getVersionUid)
            .toList();

        Map<String, FormTemplateVersionDto> versions = templateVersionRepository.findAllByUidIn(ids).stream()
            .map(versionMapper::toDto)
            .collect(Collectors.toMap(FormTemplateVersionDto::getTemplateUid, s -> s));

        return masters.map(m -> dataTemplateMapper.toInstanceDto(dataTemplateMapper.toDto(m),
            Optional.ofNullable(versions.get(m.getUid())).orElseThrow()));
    }

    public FormTemplateVersionDto findByVersion(String masterUid, int version) {
        return templateVersionRepository.findByTemplateUidAndVersionNumber(masterUid, version).map(versionMapper::toDto)
            .orElseThrow(() -> new IllegalQueryException(ErrorCode.E1114, masterUid + ":" + version));
    }

    public Page<FormTemplateVersionDto> pageVersions(String templateId, Pageable pageable) {
        Page<DataTemplateTemplateVersion> page = templateVersionRepository.findAllByTemplateUidOrderByVersionNumberDesc(templateId, pageable);
        return page.map(versionMapper::toDto);
    }

    public void applySecurityConstraints(Query query) {
        if (SecurityUtils.isSuper()) {
            return;
        }

        final CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();

        query.addCriteria(Criteria.where("templateUid").in(user.getUserFormsUIDs()));
    }

    private Page<DataTemplate> getMasterList(QueryRequest queryRequest, String jsonQueryBody) {
        final Query query = new Query();
        if (!SecurityUtils.isSuper()) {
            final CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();
            query.addCriteria(Criteria.where("uid").in(user.getUserFormsUIDs()));
        }

        if (!queryRequest.isIncludeDeleted()) {
            query.addCriteria(Criteria.where("deleted").is(false));
        }

        if (jsonQueryBody != null && !jsonQueryBody.isEmpty()) {
            try {
                FilterExpression filterExpression = UnifiedQueryParser.parse(jsonQueryBody);
                query.addCriteria(mongoQueryBuilder.buildCriteria(filterExpression));
            } catch (Exception e) {
                throw new IllegalQueryException(ErrorCode.E2050, jsonQueryBody);
            }
        }

        return getMasterQueryResult(query, queryRequest);
    }

    public Page<DataTemplate> getMasterQueryResult(Query query, QueryRequest queryRequest) {
        query.with(queryRequest.getPageable());
        final Query totalQuery = Query.of(query).limit(-1).skip(-1);

        List<DataTemplate> results = mongoTemplate.find(query, DataTemplate.class);

        Page<DataTemplate> resultsPage = PageableExecutionUtils.getPage(
            results,
            queryRequest.getPageable(),
            () -> mongoTemplate.count(totalQuery, DataTemplate.class));


        return resultsPage;
    }

    @Override
    public Optional<DataTemplateInstanceDto> findLatestByTemplate(String templateUid) {
        final var template = templateRepository.findByUid(templateUid).map(dataTemplateMapper::toDto);
        final var version = templateVersionRepository.findTopByTemplateUidOrderByVersionNumberDesc(templateUid)
            .map(versionMapper::toDto);
        if (template.isEmpty() || version.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(dataTemplateMapper.toInstanceDto(template.get(), version.get()));
    }
}
