package org.nmcpye.datarun.analytics;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.analytics.domaintabletoolkit.model.ElementColumnDefinition;
import org.nmcpye.datarun.analytics.domaintabletoolkit.model.ProjectAnalyticsMetadata;
import org.nmcpye.datarun.jpa.datatemplate.repository.TemplateElementRepository;
import org.nmcpye.datarun.jpa.project.service.ProjectService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * @author Hamza Assada
 * @since 24/08/2025
 */
@Service
@RequiredArgsConstructor
public class ElementMetadataService {
    // Inject your repository for template_element or data_element
    private final TemplateElementRepository elementConfigRepo;
    private final ProjectService projectService;


    /**
     * queries <code>template_element</code> and/or <code>DataElement</code> analytics domain elements.
     *
     * @return all project's wide elements that are considered "measures" or "dimensions"
     * for the wide view.
     */
    @Cacheable(cacheNames = "pivotableElements") // Cache this result heavily
    public ProjectAnalyticsMetadata getPivotableElements(String projectId) {
        final var project = projectService.findByUid(projectId).orElseThrow();
        final var metadata = ProjectAnalyticsMetadata.builder()
                .projectAlias(project.getUid())
                .projectName(project.getCode() != null ? project.getCode() : "");

        // TODO(Hamza) add project to template_element
        return metadata.elements(elementConfigRepo.findAll().stream()
                .map(config -> ElementColumnDefinition.builder()
                        .elementId(config.getDataElementUid())
                        .optionSetId(config.getOptionSetUid())
//                        .isMeasure(config.getIsMeasure())
//                        .isCategory(config.getIsCategory())
//                        .valueType(Boolean.TRUE.equals(config.getIsCategory()) ? AnalyticValueType.CATEGORY :
//                                AnalyticValueTypeMapper.map(config.getValueType()))
                        .columnAlias(config.getDataElementUid()).build())
                .collect(Collectors.toList())).build();
    }

}
