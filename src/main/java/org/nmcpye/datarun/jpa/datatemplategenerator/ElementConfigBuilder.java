package org.nmcpye.datarun.jpa.datatemplategenerator;

import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.datatemplate.ElementTemplateConfig;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;

/**
 * Convert field/section snapshots + metadata to
 * ElementTemplateConfig entities (in-memory only).
 *
 * @author Hamza Assada
 * @since 09/09/2025
 */
public interface ElementConfigBuilder {
    ElementTemplateConfig buildFieldConfigFromFormConf(FormDataElementConf field, PathMetadata meta,
                                                       TemplateVersion templateVersion);

    ElementTemplateConfig buildRepeatConfigFromSection(FormSectionConf section, PathMetadata meta,
                                                       TemplateVersion templateVersion);
}

