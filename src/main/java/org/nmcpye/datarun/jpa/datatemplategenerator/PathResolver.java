package org.nmcpye.datarun.jpa.datatemplategenerator;


import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;

/**
 * Resolve path metadata for a field given section definitions map.
 *
 * @author Hamza Assada
 * @since 09/09/2025
 */
public interface PathResolver {
    PathMetadata resolveForField(FormDataElementConf field);
    PathMetadata resolveForSection(FormSectionConf section);
}
