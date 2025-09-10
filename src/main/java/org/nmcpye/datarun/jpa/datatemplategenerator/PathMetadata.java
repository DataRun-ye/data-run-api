package org.nmcpye.datarun.jpa.datatemplategenerator;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Simple POJO returned by PathResolver
 *
 * @author Hamza Assada
 * @since 09/09/2025
 */
@Getter
@AllArgsConstructor
public class PathMetadata {
    private final String idPath;
    private final String namePath;
    // null if submission-level
    private final String semanticPath;
    private final Boolean hasAncestorRepeat;
    // full idPath to nearest repeatable ancestor (or null)
    private final String repeatAncestorIdPath;
    // repeat-only path to nearest repeatable ancestor
    private final String semanticRepeatAncestorPath;
}
