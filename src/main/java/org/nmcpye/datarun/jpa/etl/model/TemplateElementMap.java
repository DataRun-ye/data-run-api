package org.nmcpye.datarun.jpa.etl.model;


import lombok.Getter;
import org.nmcpye.datarun.datatemplateelement.AbstractElement;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * used by ETL service to temporarily cache the elements mapping of a {@link DataTemplateInstanceDto}
 * during processing
 *
 * @author Hamza Assada
 * @since 12/08/2025
 */
@Getter
public class TemplateElementMap {
    private final DataTemplateInstanceDto templateInstanceDto;

    /**
     * materialized repeatable paths cache, e.g. "adult.adultClassification"
     */
    private final List<String> repeatIdPaths;

    /**
     * mapping of repeatPath -> (relativeChildPath -> elementId)
     */
    private final Map<String, Map<String, String>> repeatChildrenMap;

    private final Map<String, AbstractElement> elementByIdPathMap;

    private final Map<String, AbstractElement> elementByNamePathMap;

    /**
     * (NEW) source element_template_config, used in normalization for each
     * value to reference the id of the element_template_conf that produced it.
     */
    private final Map<String, ElementTemplateConfig> elementConfigByNamePathMap;

    /**
     * elementId->path reverse map, for getting path by an elementId for retrieving categoryElement's path by its id
     */
    private final Map<String, FormDataElementConf> fieldElementReverseIdPathMap;

    /**
     * top-level fields mapping: relativePath -> elementId
     */
    private final Map<String, String> topLevelFieldIdPathToElementIdCache;

    /**
     * repeatPath → categoryElementId so ETL can set category_id easily
     */
    private final Map<String, String> repeatPathToCategoryElementIdMap;

    public TemplateElementMap(DataTemplateInstanceDto dto, Map<String, ElementTemplateConfig> elementConfigByNamePathMap) {
        this.templateInstanceDto = dto;
        this.repeatIdPaths = List.copyOf(dto.getRepeatSectionsPaths() == null ? List.of() : dto.getRepeatSectionsPaths());
        this.topLevelFieldIdPathToElementIdCache = Map.copyOf(dto.getTopLevelFieldPathToElementId() == null ? Map.of() : dto.getTopLevelFieldPathToElementId());
        this.repeatChildrenMap = dto.getRepeatSectionChildrenMap() == null ? Map.of() : dto.getRepeatSectionChildrenMap().entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Map.copyOf(e.getValue())));
        this.repeatPathToCategoryElementIdMap = Map.copyOf(dto.getRepeatCategoryElementMap() == null ? Map.of() : dto.getRepeatCategoryElementMap());
        this.elementByIdPathMap = Map.copyOf(dto.getAllElementPathMap() == null ? Map.of() : dto.getAllElementPathMap());
        this.fieldElementReverseIdPathMap = Map.copyOf(elementByIdPathMap.values()
            .stream()
            .filter(FormDataElementConf.class::isInstance)
            .map(FormDataElementConf.class::cast)
            .collect(Collectors.toMap(FormDataElementConf::getId, f -> f)));
        this.elementByNamePathMap = this.elementByIdPathMap.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey()
                    .replaceFirst(entry.getValue()
                        .getId(), entry.getValue().getName()),
                Map.Entry::getValue));
        this.elementConfigByNamePathMap = elementConfigByNamePathMap;

    }
}
