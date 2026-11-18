package org.nmcpye.datarun.jpa.datatemplate.service;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaIdentifiableService;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateVersionRepository;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
    private static final Comparator<TemplateVersion> VERSION_ORDER = Comparator
        .comparing(TemplateVersion::getTemplateUid)
        .thenComparing(TemplateVersion::getVersionNumber, Comparator.nullsFirst(Comparator.naturalOrder()));

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

    /**
     * Lists every version of the templates the user can access, paged over versions.
     * Phones replace their local version table with this list while each saved
     * submission stays pinned to the version it was captured on, so returning only
     * current versions hid older submissions and blocked their upload
     * (DataRun-ye/data-run-mobile#34). Phones start new forms on the highest
     * version they hold.
     */
    @Override
    public Page<TemplateVersion> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        QueryRequest allTemplates = new QueryRequest();
        BeanUtils.copyProperties(queryRequest, allTemplates);
        allTemplates.setPaged(false);

        List<String> templateUids = dataTemplateService.findAllByUser(allTemplates, jsonQueryBody).stream()
            .map(DataTemplate::getUid)
            .toList();
        List<TemplateVersion> versions = templateUids.isEmpty()
            ? List.of()
            : templateVersionRepository.findAllByTemplateUidIn(templateUids).stream()
                .sorted(VERSION_ORDER)
                .toList();

        Pageable pageable = queryRequest.getPageable();
        if (pageable.isUnpaged()) {
            return new PageImpl<>(versions);
        }
        int from = (int) Math.min(pageable.getOffset(), versions.size());
        int to = Math.min(from + pageable.getPageSize(), versions.size());
        return new PageImpl<>(versions.subList(from, to), pageable, versions.size());
    }

    @Override
    public Optional<TemplateVersion> findLatestByTemplate(String templateUid) {
        return templateVersionRepository.findTopByTemplateUidOrderByVersionNumberDesc(templateUid);
    }

}
