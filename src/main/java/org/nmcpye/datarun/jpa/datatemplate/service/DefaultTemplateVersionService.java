package org.nmcpye.datarun.jpa.datatemplate.service;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaIdentifiableService;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
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
    public DefaultTemplateVersionService(TemplateVersionRepository repository,
                                         CacheManager cacheManager,
                                         DataTemplateService dataTemplateService,
                                         UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
        this.templateVersionRepository = repository;
        this.dataTemplateService = dataTemplateService;
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

}
