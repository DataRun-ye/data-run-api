package org.nmcpye.datarun.jpa.datatemplategenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.datatemplate.DataType;
import org.nmcpye.datarun.jpa.datatemplate.SemanticType;
import org.nmcpye.datarun.jpa.datatemplate.TemplateElement;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Builds the in-memory inputs used to derive canonical projection metadata.
 *
 * @author Hamza Assada
 * @since 09/09/2025
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateElementBuilderImpl implements TemplateElementBuilder {

    @Override
    public TemplateElement buildTemplateElementFromField(FormDataElementConf f,
                                                         PathMetadata meta,
                                                         TemplateVersion templateVersion) {
        Objects.requireNonNull(f);
        Objects.requireNonNull(meta);
        Objects.requireNonNull(templateVersion);

        var cfg = TemplateElement.builder()
            .templateUid(templateVersion.getTemplateUid())
            .name(f.getName())
            .jsonDataPath(meta.getJsonDataPath())
            .canonicalPath(meta.getCanonicalPath())
            .dataType(DataType.fromValueType(f.getType()))
            .semanticType(SemanticType.fromValueType(f.getType()))
            .parentRepeatJsonDataPath(meta.getParentRepeatIdPath())
            .optionSetUid(f.getOptionSet())
            .displayLabel(f.getLabel());
        return cfg.build();
    }

    @Override
    public TemplateElement buildTemplateElementFromRepeat(FormSectionConf section,
                                                          PathMetadata meta,
                                                          TemplateVersion templateVersion) {
        Objects.requireNonNull(section);
        Objects.requireNonNull(meta);
        Objects.requireNonNull(templateVersion);

        var cfg = TemplateElement.builder()
            .templateUid(templateVersion.getTemplateUid())
            .dataType(DataType.ARRAY)
            .semanticType(SemanticType.Repeat);

        cfg
            .jsonDataPath(meta.getJsonDataPath())
            .name(section.getName())
            .canonicalPath(meta.getCanonicalPath())
            .parentRepeatJsonDataPath(meta.getParentRepeatIdPath())
            .displayLabel(section.getLabel());
        return cfg.build();
    }
}
