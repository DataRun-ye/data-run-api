package org.nmcpye.datarun.jpa.datatemplate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.jpa.datatemplate.mapper.DataTemplateMapper;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.nmcpye.datarun.jpa.user.repository.UserRepository;
import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion;
import org.nmcpye.datarun.mongo.datatemplateversion.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.mongo.datatemplateversion.mapper.FormJpaTemplateVersionMapper;
import org.nmcpye.datarun.mongo.datatemplateversion.mapper.FormTemplateVersionMapper;
import org.nmcpye.datarun.mongo.datatemplateversion.repository.DataTemplateVersionRepository;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/// Service Implementation for managing [DataTemplate].
@Service
@Primary
@Transactional
@Slf4j
@RequiredArgsConstructor
public class DataTemplateInstanceServiceImpl
    implements DataTemplateInstanceService {
    private final DataTemplateService dataTemplateService;
    private final DataTemplateRepository dataTemplateRepository;
    private final FormTemplateVersionMapper mongoVersionMapper;
    private final FormJpaTemplateVersionMapper jpaVersionMapper;

    private final DataTemplateVersionRepository templateVersionMongoRepository;
    private final TemplateVersionRepository jpaTemplateVersionRepository;
    private final DataTemplateMapper dataTemplateMapper;

    @Transactional
    public DataTemplateInstanceDto saveNewVersion(DataTemplateInstanceDto dto) {
        DataTemplate maybeNew = dataTemplateMapper.fromInstanceDto(dto);

        DataTemplate template = dataTemplateRepository
                .findByUidForWrite(maybeNew.getUid())   // PESSIMISTIC_WRITE lock
                .map(existing -> {
                    existing.setVersionNumber(existing.getVersionNumber() + 1);
                    // keep other fields as needed
                    return existing;
                })
                .orElseGet(() -> {
                    maybeNew.setVersionNumber(1);
                    return maybeNew;
                });

        int newVersion = template.getVersionNumber();

        // Build new version entity
        TemplateVersion versionEntity = jpaVersionMapper.fromInstanceDto(dto);
        versionEntity.setVersionNumber(newVersion);
        versionEntity.setTemplateUid(template.getUid());
        // generate id/id if you have a generator; keep same id semantics
        if (versionEntity.getId() == null) {
            versionEntity.setId(CodeGenerator.nextUlid());
        }
        if (versionEntity.getUid() == null) {
            versionEntity.setUid(CodeGenerator.generateUid());
        }

        // Save version first so we ensure it has PK/id for the master row
        TemplateVersion savedVersion = jpaTemplateVersionRepository.save(versionEntity);

        // update template to reference new version id and persist
        template.setVersionUid(savedVersion.getUid());
        template.setVersionNumber(newVersion);
        DataTemplate savedTemplate = dataTemplateService.save(template); // still within @Transactional

        // both saved, return DTO
        return dataTemplateMapper.toInstanceDto(
                dataTemplateMapper.toDto(savedTemplate),
                jpaVersionMapper.toDto(savedVersion)
        );
    }

    /// Create a brand‐new version of the FormTemplate (Postgres ↔ Mongo) as a single “unit.”
    /// If anything fails, neither side is left half‐updated.
    @Transactional
    public DataTemplateInstanceDto saveNewVersionDeprecated(DataTemplateInstanceDto dto) {
        DataTemplate maybeNew = dataTemplateMapper.fromInstanceDto(dto);

        //    We call a custom repository method findByUidForWrite(…) that uses a PESSIMISTIC_WRITE lock
        //    so that two concurrent callers cannot both see versionNumber = N and then both flip to N+1.
        // postgresql
        DataTemplate template = dataTemplateRepository
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
        DataTemplateVersion versionDoc = mongoVersionMapper.fromInstanceDto(dto)
            .version(newVersion)
            .templateUid(template.getUid());

        DataTemplateVersion savedVersion;
        try {
            savedVersion = templateVersionMongoRepository.save(versionDoc);
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
            savedTemplate = dataTemplateService.save(template);
            // At this point, we’ve done **two** modifications to Postgres in the same @Transactional:
            //   • either an UPDATE that bumped versionNumber on an existing row (and now sets formVersionUid),
            //   • or an INSERT of a brand‐new row with (uid, versionNumber=1, formVersionUid=<new Mongo UID>).
            //
            // If this save(…) throws (e.g. a unique‐constraint violation, database schema error, etc.), we must delete the Mongo doc.
            // Catch that exception, delete the Mongo version, and rethrow, so that Postgres rolls back its UPDATE/INSERT.
        } catch (RuntimeException postgresEx) {
            // COMPENSATE: remove the Mongo doc we just created, because Postgres did not accept the final save.
            try {
                templateVersionMongoRepository.deleteById(savedVersion.getUid());
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
            mongoVersionMapper.toDto(savedVersion)
        );
    }

    @Override
    public List<DataTemplateInstanceDto> findAllByUidIn(Collection<String> uids) {
        final var masters = dataTemplateRepository.findAllByUidIn(uids);
        // batch-load versions
        List<String> ids = masters.stream()
            .map(DataTemplate::getVersionUid)
            .toList();

        Map<String, FormTemplateVersionDto> versions = templateVersionMongoRepository.findAllByUidIn(ids).stream()
            .map(mongoVersionMapper::toDto)
            .collect(Collectors.toMap(FormTemplateVersionDto::getTemplateUid, s -> s));

        return masters.stream().map(m ->
            dataTemplateMapper.toInstanceDto(dataTemplateMapper.toDto(m),
                Optional.ofNullable(versions.get(m.getUid())).orElseThrow(
                    () -> new IllegalQueryException(ErrorCode.E1120, m.getUid())
                ))).toList();
    }

    @Override
    public List<DataTemplateInstanceDto> findAll() {
        final var masters = dataTemplateRepository.findAll();
        // batch-load versions
        List<String> ids = masters.stream()
            .map(DataTemplate::getVersionUid)
            .toList();

        Map<String, FormTemplateVersionDto> versions = templateVersionMongoRepository.findAllByUidIn(ids).stream()
            .map(mongoVersionMapper::toDto)
            .collect(Collectors.toMap(FormTemplateVersionDto::getTemplateUid, s -> s));

        return masters.stream().map(m ->
            dataTemplateMapper.toInstanceDto(dataTemplateMapper.toDto(m),
                Optional.ofNullable(versions.get(m.getUid())).orElseThrow(
                    () -> new IllegalQueryException(ErrorCode.E1120, m.getUid())
                ))).toList();
    }

    @Override
    public Page<DataTemplateInstanceDto> findAllByUidIn(Collection<String> uids, Pageable pageable) {
        final var masters = dataTemplateRepository.findAllByUidIn(uids, pageable);
        // batch-load versions
        List<String> ids = masters.stream()
            .map(DataTemplate::getVersionUid)
            .toList();

        Map<String, FormTemplateVersionDto> versions = templateVersionMongoRepository.findAllByUidIn(ids).stream()
            .map(mongoVersionMapper::toDto)
            .collect(Collectors.toMap(FormTemplateVersionDto::getTemplateUid, s -> s));

        return masters.map(m -> dataTemplateMapper.toInstanceDto(dataTemplateMapper.toDto(m),
            Optional.ofNullable(versions.get(m.getUid())).orElseThrow(
                () -> new IllegalQueryException(ErrorCode.E1120, m.getUid())
            )));
    }

    @Override
    public Page<DataTemplateInstanceDto> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        // load only lightweight masters
        Page<DataTemplate> masters = dataTemplateService.findAllByUser(queryRequest, jsonQueryBody);
        // batch-load versions
        List<String> ids = masters.stream()
            .map(DataTemplate::getVersionUid)
            .toList();

        Map<String, FormTemplateVersionDto> versions = templateVersionMongoRepository.findAllByUidIn(ids).stream()
            .map(mongoVersionMapper::toDto)
            .collect(Collectors.toMap(FormTemplateVersionDto::getTemplateUid, s -> s));

        return masters.map(m -> dataTemplateMapper.toInstanceDto(dataTemplateMapper.toDto(m),
            Optional.ofNullable(versions.get(m.getUid())).orElseThrow(
                () -> new IllegalQueryException(ErrorCode.E1120, m.getUid())
            )));
    }

//    @Transactional
//    @Override
//    public void migrateDataFormTemplateVersion(DataFormTemplate formTemplate) {
//        if (!dataTemplateRepository.existsByUid(formTemplate.getUid())) {
//            final var formTemplateVersion = formTemplate.getVersion();
//            final var templateUid = formTemplate.getUid();
//
//            final DataTemplateInstanceDto dataTemplateInstanceDto = dataFormTemplateMapper.toDto(formTemplate);
//            final DataTemplate template = dataTemplateMapper.fromInstanceDto(dataTemplateInstanceDto);
//
//            final var newVersionUid = CodeGenerator.generateUid();
//
//            dataTemplateService.save(template.versionNumber(formTemplateVersion)
//                // temporary for migrating old DataFormTemplate
//                .uid(templateUid)
//                .versionNumber(formTemplateVersion)
//                .versionUid(newVersionUid));
//
//            templateVersionRepository.save(versionMapper
//                .fromInstanceDto(dataTemplateInstanceDto)
//                .uid(newVersionUid)
//                .version(formTemplateVersion)
//                .templateUid(templateUid));
//        }
//    }

    @CacheEvict(cacheNames = {
        UserRepository.USER_TEAM_FORM_ACCESS_CACHE,
        UserRepository.USER_ACTIVITY_IDS_CACHE,
        UserRepository.USER_TEAM_IDS_CACHE,
        DataTemplateVersionRepository.TEMPLATE_UID_LATEST_VERSION_CACHE,
        DataTemplateRepository.TEMPLATE_BY_UID_CACHE,

    })
    @Override
    public DataTemplateInstanceDto save(DataTemplateInstanceDto dataTemplateInstanceDto) {
        return saveNewVersion(dataTemplateInstanceDto);
    }

    @Override
    public boolean existsByUid(String uid) {
        return findLatestByTemplate(uid).isPresent();
    }

    @Override
    public Optional<DataTemplateInstanceDto> findByUid(String uid) {
        return findLatestByTemplate(uid);
    }

    @CacheEvict(cacheNames = {
        UserRepository.USER_TEAM_FORM_ACCESS_CACHE,
        UserRepository.USER_ACTIVITY_IDS_CACHE,
        UserRepository.USER_TEAM_IDS_CACHE,
        DataTemplateVersionRepository.TEMPLATE_UID_LATEST_VERSION_CACHE,
        DataTemplateRepository.TEMPLATE_BY_UID_CACHE,
    })
    @Override
    public void deleteByUid(String uid) {
        dataTemplateService.deleteByUid(uid);
    }

    @CacheEvict(cacheNames = {
        UserRepository.USER_TEAM_FORM_ACCESS_CACHE,
        UserRepository.USER_ACTIVITY_IDS_CACHE,
        UserRepository.USER_TEAM_IDS_CACHE,
        DataTemplateVersionRepository.TEMPLATE_UID_LATEST_VERSION_CACHE,
        DataTemplateRepository.TEMPLATE_BY_UID_CACHE,
    })
    @Override
    public DataTemplateInstanceDto update(DataTemplateInstanceDto dataTemplateInstanceDto) {
        dataTemplateRepository.findByUid(dataTemplateInstanceDto.getUid()).orElseThrow(() ->
            new IllegalQueryException(ErrorCode.E1113, dataTemplateInstanceDto.getUid()));
        return save(dataTemplateInstanceDto);
    }

    @Override
    public Optional<DataTemplateInstanceDto> findLatestByTemplate(String templateUid) {
        final var template = dataTemplateService.findByUid(templateUid).map(dataTemplateMapper::toDto);
        final var version = templateVersionMongoRepository.findTopByTemplateUidOrderByVersionNumberDesc(templateUid)
            .map(mongoVersionMapper::toDto);
        if (template.isEmpty() || version.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(dataTemplateMapper.toInstanceDto(template.get(), version.get()));
    }

    @Override
    public Optional<DataTemplateInstanceDto> findByTemplateAndVersionUid(String templateUid,
                                                                         String versionUid) {
        final var template = dataTemplateService.findByUid(templateUid).map(dataTemplateMapper::toDto);
        final var version = templateVersionMongoRepository.findByTemplateUidAndUid(templateUid, versionUid)
            .map(mongoVersionMapper::toDto);

        if (template.isEmpty() || version.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(dataTemplateMapper.toInstanceDto(template.get(), version.get()));
    }

    @Override
    public Optional<DataTemplateInstanceDto> findByTemplateAndVersionNo(String templateUid, Integer versionNumber) {
        final var template = dataTemplateService.findByUid(templateUid).map(dataTemplateMapper::toDto);
        final var version =
            templateVersionMongoRepository.findByTemplateUidAndVersionNumber(templateUid, versionNumber)
                .map(mongoVersionMapper::toDto);

        if (template.isEmpty() || version.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(dataTemplateMapper.toInstanceDto(template.get(), version.get()));
    }

    @Override
    public void delete(DataTemplateInstanceDto object) {
        deleteByUid(object.getUid());
    }
}
