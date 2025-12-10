package org.nmcpye.datarun.analytics;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.analytics.domaintabletoolkit.model.ActivityAnalyticsMetadata;
import org.nmcpye.datarun.analytics.domaintabletoolkit.model.CeMeta;
import org.nmcpye.datarun.etl.repository.DimAssignmentJdbcRepository;
import org.nmcpye.datarun.jpa.activity.repository.ActivityRepository;
import org.nmcpye.datarun.jpa.datatemplate.repository.CanonicalElementRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada
 * @since 24/08/2025
 */
@Service
@RequiredArgsConstructor
public class ElementMetadataService {
    // Inject your repository for template_element or data_element
    private final CanonicalElementRepository elementRepository;
    private final ActivityRepository activityRepository;
    private final DimAssignmentJdbcRepository assignmentJdbcRepository;

    /**
     * queries <code>template_element</code> and/or <code>DataElement</code> analytics domain elements.
     *
     * @return all project's wide elements that are considered "measures" or "dimensions"
     * for the wide view.
     */
    @Cacheable(cacheNames = "pivotableElements") // Cache this result heavily
    public ActivityAnalyticsMetadata getPivotableElements(String activityUid) {
        final var activity = activityRepository.findByUid(activityUid).orElseThrow();
        final var metadata = ActivityAnalyticsMetadata.builder()
            .activityAlias(activity.getUid())
            .activityName(activity.getCode() != null ? activity.getCode() : "");

        final List<String> activityTemplateUids = assignmentJdbcRepository.templatesByActivity(activity.getUid());

        return metadata.elements(elementRepository.findByTemplateUidIn(activityTemplateUids).stream()
            .map(ce -> CeMeta.builder()
                .elementId(ce.getId())
                .templateUid(ce.getTemplateUid())
                .optionSetUid(ce.getOptionSetUid())
                .columnAlias(ce.getSafeName())
                .dataType(ce.getDataType())
                .semanticType(ce.getSemanticType()).build())
            .collect(Collectors.toList())).build();
    }
}
