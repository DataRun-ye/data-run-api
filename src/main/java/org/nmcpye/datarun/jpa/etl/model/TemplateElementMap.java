package org.nmcpye.datarun.jpa.etl.model;


import lombok.Getter;
import org.nmcpye.datarun.datatemplateelement.AbstractElement;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * used by ETL service to temporarily cache the elements mapping of a {@link DataTemplateInstanceDto}
 * during processing
 *
 * @author Hamza Assada 12/08/2025 (7amza.it@gmail.com)
 */
@Getter
public class TemplateElementMap {
    private final DataTemplateInstanceDto templateInstanceDto;

    /**
     * materialized repeatable paths cache, e.g. "adult.adultClassification"
     */
    private final List<String> repeatPathsCache;

    /**
     * mapping of repeatPath -> (relativeChildPath -> elementId)
     */
    private final Map<String, Map<String, String>> repeatChildrenMapCache;

    private final Map<String, AbstractElement> elementPathMapCache;

    private final Map<String, AbstractElement> elementNamePathMapCache;

    /**
     * elementId->path reverse map, for getting path by an elementId for retrieving categoryElement's path by its id
     */
    private final Map<String, String> fieldElementReversePathMap;

    /**
     * top-level fields mapping: relativePath -> elementId
     */
    private final Map<String, String> topLevelFieldPathToElementIdCache;

    /**
     * repeatPath → categoryElementId so ETL can set category_id easily
     */
    private final Map<String, String> repeatCategoryElementMapCache;

    public TemplateElementMap(DataTemplateInstanceDto dto) {
        this.templateInstanceDto = dto;
        this.repeatPathsCache = List.copyOf(dto.getRepeatSectionsPaths() == null ? List.of() : dto.getRepeatSectionsPaths());
        this.topLevelFieldPathToElementIdCache = Map.copyOf(dto.getTopLevelFieldPathToElementId() == null ? Map.of() : dto.getTopLevelFieldPathToElementId());
        this.repeatChildrenMapCache = dto.getRepeatSectionChildrenMap() == null ? Map.of() : dto.getRepeatSectionChildrenMap().entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Map.copyOf(e.getValue())));
        this.repeatCategoryElementMapCache = Map.copyOf(dto.getRepeatCategoryElementMap() == null ? Map.of() : dto.getRepeatCategoryElementMap());
        this.elementPathMapCache = Map.copyOf(dto.getAllElementPathMap() == null ? Map.of() : dto.getAllElementPathMap());
        this.fieldElementReversePathMap = Map.copyOf(elementPathMapCache.values()
            .stream()
            .filter(FormDataElementConf.class::isInstance)
            .map(FormDataElementConf.class::cast)
            .collect(Collectors.toMap(FormDataElementConf::getId, AbstractElement::getPath)));
        this.elementNamePathMapCache = this.elementPathMapCache.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey()
                    .replaceFirst(entry.getValue()
                        .getId(), entry.getValue().getName()),
                Map.Entry::getValue));

    }

    /**
     * @param repeatPath repeat section path
     * @return the path children
     */
    public Map<String, String> getChildrenFieldPathsUnderRepeat(String repeatPath) {
        return repeatChildrenMapCache.getOrDefault(repeatPath, Map.of());
    }
}
