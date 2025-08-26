package org.nmcpye.datarun.analytics;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.analytics.domaintabletoolkit.model.AnalyticValueType;
import org.nmcpye.datarun.analytics.domaintabletoolkit.model.AnalyticValueTypeMapper;
import org.nmcpye.datarun.analytics.domaintabletoolkit.model.ElementColumnDefinition;
import org.nmcpye.datarun.analytics.domaintabletoolkit.model.ProjectAnalyticsMetadata;
import org.nmcpye.datarun.jpa.datatemplate.repository.ElementTemplateConfigRepository;
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
    // Inject your repository for element_template_config or data_element
    private final ElementTemplateConfigRepository elementConfigRepo;
    private final ProjectService projectService;


    /**
     * queries <code>element_template_config</code> and/or <code>DataElement</code> analytics domain elements.
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

        // TODO(Hamza) add project to element_template_config
        return metadata.elements(elementConfigRepo.findAll().stream()
                .map(config -> ElementColumnDefinition.builder()
                        .elementId(config.getDataElementId())
                        .isMeasure(config.getIsMeasure())
                        .isCategory(config.getIsCategory())
                        .optionSetId(config.getOptionSetId())
                        .valueType(Boolean.TRUE.equals(config.getIsCategory()) ? AnalyticValueType.CATEGORY :
                                AnalyticValueTypeMapper.map(config.getValueType()))
                        .columnAlias(config.getDataElementId()).build())
                .collect(Collectors.toList())).build();
    }

}
