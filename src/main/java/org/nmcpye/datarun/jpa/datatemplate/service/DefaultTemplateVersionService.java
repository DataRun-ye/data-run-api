package org.nmcpye.datarun.jpa.datatemplate.service;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaIdentifiableService;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.mapper.DataTemplateMapper;
import org.nmcpye.datarun.jpa.datatemplate.mapper.FormJpaTemplateVersionMapper;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.nmcpye.datarun.mongo.datatemplateversion.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link TemplateVersion}.
 */
@Service
@Primary
@Transactional
@Slf4j
@SuppressWarnings("unused")
public class DefaultTemplateVersionService
    extends DefaultJpaIdentifiableService<TemplateVersion>
    implements TemplateVersionService {
    private final DataTemplateService dataTemplateService;
    private final TemplateVersionRepository templateVersionRepository;
    private final FormJpaTemplateVersionMapper versionMapper;

    private final DataTemplateRepository templateRepository;
    private final DataTemplateMapper dataTemplateMapper;

    public DefaultTemplateVersionService(TemplateVersionRepository repository,
                                         CacheManager cacheManager,
                                         DataTemplateService dataTemplateService,
                                         UserAccessService userAccessService,
                                         FormJpaTemplateVersionMapper versionMapper,
                                         DataTemplateRepository templateRepository,
                                         @Lazy DataTemplateMapper dataTemplateMapper) {
        super(repository, cacheManager, userAccessService);
        this.templateVersionRepository = repository;
        this.dataTemplateService = dataTemplateService;
        this.versionMapper = versionMapper;
        this.templateRepository = templateRepository;
        this.dataTemplateMapper = dataTemplateMapper;
    }

    @Override
    public Page<TemplateVersion> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        // load only lightweight masters
        Page<DataTemplate> masters = dataTemplateService.findAllByUser(queryRequest, jsonQueryBody);
        // batch-load versions
        List<String> ids = masters.stream()
            .map(DataTemplate::getVersionUid)
            .toList();

        Map<String, TemplateVersion> versions = templateVersionRepository.findAllByUidIn(ids).stream()
            .collect(Collectors.toMap(TemplateVersion::getTemplateUid, Function.identity()));

        return masters.map(m -> versions.get(m.getUid()));
    }

    @Override
    public Optional<TemplateVersion> findLatestByTemplate(String templateUid) {
        return templateVersionRepository.findTopByTemplateUidOrderByVersionNumberDesc(templateUid);
    }

    @Override
    public FormTemplateVersionDto findByVersion(String masterUid, int version) {
        return templateVersionRepository.findByTemplateUidAndVersionNumber(masterUid, version).map(versionMapper::toDto)
            .orElseThrow(() -> new IllegalQueryException(ErrorCode.E1114, masterUid + ":" + version));
    }

    @Override
    public Page<FormTemplateVersionDto> pageVersions(String templateId, Pageable pageable) {
        Page<TemplateVersion> page = templateVersionRepository.findAllByTemplateUidOrderByVersionNumberDesc(templateId, pageable);
        return page.map(versionMapper::toDto);
    }
}
