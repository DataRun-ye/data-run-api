package org.nmcpye.datarun.jpa.datatemplategenerator;

import org.nmcpye.datarun.datatemplateelement.FieldTemplateElementDto;
import org.nmcpye.datarun.datatemplateelement.SectionTemplateElementDto;
import org.nmcpye.datarun.jpa.datatemplate.TemplateElement;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersion;

/**
 * Convert field/section snapshots + metadata to
 * TemplateElement entities (in-memory only).
 *
 * @author Hamza Assada
 * @since 09/09/2025
 */
public interface TemplateElementBuilder {
    TemplateElement buildTemplateElementFromField(FieldTemplateElementDto field,
                                                  PathMetadata meta,
                                                  TemplateVersion templateVersion);

    TemplateElement buildTemplateElementFromRepeat(SectionTemplateElementDto section, PathMetadata meta,
                                                   TemplateVersion templateVersion);
}

