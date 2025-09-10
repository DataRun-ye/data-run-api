package org.nmcpye.datarun.jpa.datatemplategenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.datatemplateelement.AbstractElement;
import org.nmcpye.datarun.datatemplateelement.AggregationType;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builds ElementTemplateConfig instances. Keeps responsibility small: mapping / normalization.
 * Persistence is delegated to Publisher.
 *
 * @author Hamza Assada
 * @since 09/09/2025
 */
@Component
@Slf4j
public class ElementConfigBuilderImpl implements ElementConfigBuilder {

    private final ObjectMapper objectMapper;

    public ElementConfigBuilderImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ElementTemplateConfig buildFieldConfigFromFormConf(FormDataElementConf f,
                                                              PathMetadata meta,
                                                              TemplateVersion templateVersion) {
        Objects.requireNonNull(f);
        Objects.requireNonNull(meta);
        Objects.requireNonNull(templateVersion);

        var cfg = ElementTemplateConfig.builder()
            .templateUid(templateVersion.getTemplateUid())
            .templateVersionUid(templateVersion.getUid())
            .versionNo(templateVersion.getVersionNumber())
            .elementKind(ElementTemplateConfig.ElementKind.FIELD)
            // canonical data element uid (FormElementConf.id == DataElement.uid)
            .dataElementUid(f.getId())
            .name(f.getName())
            // paths
            .idPath(meta.getIdPath())
            .namePath(meta.getNamePath())
            .semanticPath(meta.getSemanticPath())
            .hasRepeatAncestor(meta.getHasAncestorRepeat())
            .ancestorRepeatPath(meta.getRepeatAncestorIdPath())
            .ancestorRepeatSemanticPath(meta.getSemanticRepeatAncestorPath())
            // copy simple flags with precedence: template override (FormDataElementConf) -> canonical DE (assumed copied into conf)
            .isMulti(isSelectMulti(f))
            .isMeasure(resolveIsMeasure(f))
            .isDimension(resolveIsDimension(f))
            .aggregationType(resolveAggregationType(f))
            .sortOrder(f.getOrder())
            // copy immutable canonical metadata (available in FormDataElementConf snapshot)
            .valueType(f.getType())
            .optionSetUid(f.getOptionSet())
            // labels & definition snapshot (store as whatever structure definition holds)
            .displayLabel(f.getLabel())
            .definitionJson(getDefinitionSnapshot(f))
            // compute deterministic uid for config row and path_hash
            .pathHash(HashUtil.hashToLong(templateVersion.getUid() + ":" + meta.getIdPath()))
//            .uid(generateElementConfigUid(templateVersion.getUid(), meta.getIdPath()))
            .isReference(determineIsReference(f));

        // default audit fields left to publisher or DB triggers
        return cfg.build();
    }

    @Override
    public ElementTemplateConfig buildRepeatConfigFromSection(FormSectionConf section,
                                                              PathMetadata meta,
                                                              TemplateVersion templateVersion) {
        Objects.requireNonNull(section);
        Objects.requireNonNull(meta);
        Objects.requireNonNull(templateVersion);

        var cfg = ElementTemplateConfig.builder().templateUid(templateVersion.getTemplateUid()).templateVersionUid(templateVersion.getUid())
            .versionNo(templateVersion.getVersionNumber())

            .elementKind(ElementTemplateConfig.ElementKind.REPEAT);


        // synthetic dataElementUid for repeat grain
        String syntheticUid = "RPT_" + Math.abs(HashUtil.hashToLong(templateVersion.getUid() + ":" + meta.getSemanticPath()));
        cfg.dataElementUid(syntheticUid)
            .idPath(meta.getIdPath())
            .namePath(meta.getNamePath())
            .name(section.getName())
            .semanticPath(meta.getSemanticPath())
            .hasRepeatAncestor(meta.getHasAncestorRepeat())
            .ancestorRepeatPath(meta.getRepeatAncestorIdPath())
            .ancestorRepeatSemanticPath(meta.getSemanticRepeatAncestorPath())
            .definitionJson(getDefinitionSnapshot(section))
            .displayLabel(section.getLabel())
            .sortOrder(section.getOrder())
            .pathHash(HashUtil.hashToLong(templateVersion.getUid() + ":" + meta.getIdPath()))
//            .uid(generateElementConfigUid(templateVersion.getUid(), meta.getIdPath()))
            // repeat identity config: prefer explicit properties on section, fallback to defaults
//                .repeatIdField(section.getRepeatIdField() != null ? section.getRepeatIdField() : "_id")
//                .repeatIndexField(section.getRepeatIndexField() != null ? section.getRepeatIndexField() : "_index")
//                .repeatGenerateStrategy(section.getRepeatGenerateStrategy() != null ? section.getRepeatGenerateStrategy() : "SERVER_UUID")

            // semantic grain points to this repeat
            .semanticGrain("repeat:" + meta.getIdPath());

        return cfg.build();
    }

    // ---------- helper methods (small) ------------

    private boolean isSelectMulti(FormDataElementConf f) {
        return f.getType() == ValueType.SelectMulti;
    }

    private Boolean resolveIsMeasure(FormDataElementConf f) {
        // prefer explicit conf override if present; assume FormDataElementConf may carry a Boolean
        if (f.getIsMeasure() != null) return f.getIsMeasure();
        // fallback to false if missing — (or true depending on your system)
        return Boolean.TRUE; // your platform default earlier was true
    }

    private Boolean resolveIsDimension(FormDataElementConf f) {
        if (f.getIsDimension() != null) return f.getIsDimension();
        return Boolean.FALSE;
    }

    private AggregationType resolveAggregationType(FormDataElementConf f) {
        // try conf override, then fallback to a safe default SUM
        if (f.getAggregationType() != null) {
            return f.getAggregationType();
        }
        return getDefaultAggregationType(f.getType());
    }

    private JsonNode getDefinitionSnapshot(AbstractElement conf) {
        // If conf already serializes a Map/POJO with rules, return that; otherwise return a small snapshot
        Map<String, Object> snapshot = new HashMap<>();
        try {
            return objectMapper.convertValue(conf, JsonNode.class);
        } catch (Exception ex) {
            log.error("error mapping element definition json, path:{}", conf.getPath());
            return null;
        }
    }

    private boolean determineIsReference(FormDataElementConf f) {
        return f.getType().isSystemReferenceType();
    }

    private String generateElementConfigUid(String templateVersionUid, String idPath) {
        long h = HashUtil.hashToLong(templateVersionUid + ":" + idPath);
        // short base36 encoding for readability
        return "E" + Long.toUnsignedString(h, 36);
    }

    private String referenceTableFor(ValueType vt) {
        if (vt == null) return null;
        return switch (vt) {
            case Activity -> "activity";
            case Team -> "team";
            case OrganisationUnit -> "org_unit";
            case Entity -> "entity_instance";
            case SelectOne, SelectMulti -> "option_value";
            default -> null;
        };
    }

    private static AggregationType getDefaultAggregationType(ValueType valueType) {
        return switch (valueType) {
            case Number, Integer, Percentage, UnitInterval, IntegerPositive,
                 IntegerNegative, IntegerZeroOrPositive -> AggregationType.SUM;
            case Boolean, TrueOnly -> AggregationType.SUM_TRUE;
            default -> AggregationType.COUNT;
        };
    }
}
