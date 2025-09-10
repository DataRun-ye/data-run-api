package org.nmcpye.datarun.jpa.datatemplategenerator;

import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;

import java.util.*;
import java.util.stream.Collectors;

/// MaterializedPathResolver computes PathMetadata for both fields and sections
/// using the authoritative materialized path present in the element DTOs and the
/// provided sectionByName lookup (section name -> FormSectionConf).
///
/// Rules implemented:
/// - `idPath`: authoritative element.getPath() with final segment normalized to field.getId() for fields.
/// - `namePath`: same as idPath for sections; for fields replace last segment with field.getName() (structural name).
/// - `semanticPath`: join only repeatable segments (repeat-only path) + element leaf (section name or field id).
/// If there are no repeatable ancestors and element is not repeatable, semanticPath is null.
/// - `repeatAncestorIdPath`: the full idPath of the nearest repeatable ancestor (the array in JSON that contains the element).
/// - `semanticRepeatAncestorPath`: join only repeatable ancestor segment names up to the nearest repeatable ancestor.
/// - `hasRepeatableAncestor`: true if any repeatable ancestor exists (for elements nested in repeats).
///
/// @author Hamza Assada
/// @since 09/09/2025
public class MaterializedPathResolver implements PathResolver {

    private final Map<String, FormSectionConf> sectionByName;

    /**
     * @param sectionByName map keyed by section.getName() - can be empty but must not be null
     */
    public MaterializedPathResolver(Map<String, FormSectionConf> sectionByName) {
        this.sectionByName = sectionByName == null ? Collections.emptyMap() : sectionByName;
    }

    // ------------------- PUBLIC API -------------------

    @Override
    public PathMetadata resolveForField(FormDataElementConf field) {
        Objects.requireNonNull(field, "field required");

        // authoritative raw path (may be "DE1" or "household.children.DE1")
        String rawPath = field.getPath();
        if (rawPath == null || rawPath.isBlank()) {
            String parent = field.getParent();
            rawPath = (parent == null || parent.isBlank()) ? field.getId() : parent + "." + field.getId();
        }

        List<String> segments = splitSegments(rawPath);

        // ensure last segment equals DataElement UID (field.getId())
        String leafUid = field.getId();
        if (segments.isEmpty()) {
            segments = new ArrayList<>(Collections.singletonList(leafUid));
        } else {
            segments.set(segments.size() - 1, leafUid);
        }
        String idPath = String.join(".", segments);

        // namePath: replace final segment with structural name (field.getName())
        String namePath = PathUtils.computeNamePath(idPath, field.getName());

        // Determine repeatable ancestors by checking sections for each non-final segment
        RepeatAnalysis ra = analyzeRepeatSegments(segments, /*lastIndexExclusive*/ segments.size() - 1);

        boolean hasRepeatableAncestor = !ra.repeatableSegments.isEmpty();
        String semanticPath = null;
        if (hasRepeatableAncestor) {
            List<String> sem = new ArrayList<>(ra.repeatableSegments);
            sem.add(leafUid); // leaf is the dataElement uid (ties to canonical DE)
            semanticPath = String.join(".", sem);
        } else {
            semanticPath = leafUid;
        }

        // nearest repeatable ancestor idPath is full idPath up to that segment
        String repeatAncestorIdPath = ra.nearestRepeatAncestorFullPath;
        String semanticRepeatAncestorPath = ra.semanticRepeatAncestorPath;

        return new PathMetadata(idPath, namePath, semanticPath, hasRepeatableAncestor, repeatAncestorIdPath, semanticRepeatAncestorPath);
    }

    @Override
    public PathMetadata resolveForSection(FormSectionConf section) {
        Objects.requireNonNull(section, "section required");

        // authoritative raw path (sections also have materialized paths)
        String rawPath = section.getPath();
        if (rawPath == null || rawPath.isBlank()) {
            rawPath = section.getName();
        }

        List<String> segments = splitSegments(rawPath);
        String idPath = String.join(".", segments);
        String namePath = idPath; // sections use structural name segments; namePath == idPath

        // Determine repeatable ancestors by checking all segments EXCLUDING the final segment? We want
        // repeatable ancestors above this section, but semantic path for this section must include
        // repeatable ancestors plus the section itself if it's repeatable.
        RepeatAnalysis ra = analyzeRepeatSegments(segments, /*lastIndexExclusive*/ segments.size()); // include all segments in analysis

        // Now compute semantic path for the section:
        // - If there are repeatable segments, semantic path is join(repeatable segments up to this section).
        // - If no repeatable ancestors and the section itself is repeatable -> semanticPath = section.name
        String semanticPath = null;
        if (!ra.repeatableSegments.isEmpty()) {
            // If section name is itself repeatable but wasn't the last repeat segment in ra's list, ensure it's included.
            List<String> sem = new ArrayList<>(ra.repeatableSegments);
            // If last repeatable segment is not this section's name and this section is repeatable, include it
            if (Boolean.TRUE.equals(section.getRepeatable()) && (sem.isEmpty() || !sem.get(sem.size() - 1).equals(section.getName()))) {
                sem.add(section.getName());
            }
            semanticPath = String.join(".", sem);
        } else if (Boolean.TRUE.equals(section.getRepeatable())) {
            semanticPath = section.getName();
        }

        // nearest repeatable ancestor is the repeatable ancestor above (not itself)
        // For sections we want the nearest repeatable ancestor *above* this section, so compute that:
        String nearestAncestorAbove = findNearestRepeatAncestorAbove(segments);

        // semanticRepeatAncestorPath: semantic-repeat-only path for nearestAncestorAbove
        String semanticRepeatAncestorPath = null;
        if (nearestAncestorAbove != null) {
            // build list of repeatable segments up to that ancestor
            List<String> prefixSegments = segmentsUpTo(segments, nearestAncestorAbove);
            List<String> repeatOnly = prefixSegments.stream()
                .filter(s -> sectionByName.containsKey(s) &&
                    Boolean.TRUE.equals(sectionByName.get(s).getRepeatable()))
                .collect(Collectors.toList());
            if (!repeatOnly.isEmpty()) {
                semanticRepeatAncestorPath = String.join(".", repeatOnly);
            }
        }

        boolean hasRepeatableAncestor = ra.repeatableSegments.stream()
            .anyMatch(seg -> !seg.equals(section.getName())); // true if there is a repeatable ancestor above

        return new PathMetadata(idPath, namePath, semanticPath, hasRepeatableAncestor, nearestAncestorAbove, semanticRepeatAncestorPath);
    }

    // ------------------- helpers -------------------

    // Split path into clean segments
    private static List<String> splitSegments(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return Collections.emptyList();
        return Arrays.stream(rawPath.split("\\."))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    /**
     * Analyze repeatability across non-final segments (or up to lastIndexExclusive).
     * Returns:
     * - repeatableSegments: list of the repeatable segment names in order (only segments that are repeatable)
     * - nearestRepeatAncestorFullPath: the full idPath (join of segments) to the nearest repeatable ancestor (closest)
     * - semanticRepeatAncestorPath: join of repeatable segment names up to nearest
     */
    private RepeatAnalysis analyzeRepeatSegments(List<String> segments, int lastIndexExclusive) {
        List<String> repeatableSegments = new ArrayList<>();
        String nearestRepeatAncestorFullPath = null;
        String semanticRepeatAncestorPath = null;

        // examine segments up to lastIndexExclusive (exclusive)
        for (int i = 0; i < Math.max(0, Math.min(lastIndexExclusive, segments.size())); i++) {
            String seg = segments.get(i);
            FormSectionConf s = sectionByName.get(seg);
            if (s != null && Boolean.TRUE.equals(s.getRepeatable())) {
                repeatableSegments.add(seg);
                // nearestRepeatAncestorFullPath should become the most recent repeatable ancestor's full path
                nearestRepeatAncestorFullPath = String.join(".", segments.subList(0, i + 1));
                // semanticRepeatAncestorPath is join of repeatable segments so far
                semanticRepeatAncestorPath = String.join(".", repeatableSegments);
            }
        }

        return new RepeatAnalysis(repeatableSegments, nearestRepeatAncestorFullPath, semanticRepeatAncestorPath);
    }

    // For sections: find nearest repeatable ancestor *above* this section (i.e., among earlier segments)
    private String findNearestRepeatAncestorAbove(List<String> segments) {
        for (int i = segments.size() - 1; i >= 0; i--) {
            String seg = segments.get(i);
            FormSectionConf s = sectionByName.get(seg);
            if (s != null && Boolean.TRUE.equals(s.getRepeatable())) {
                // if this is the final segment and equals the current section name, skip because that's the section itself
                // the caller will use this method when they need an ancestor above
                return String.join(".", segments.subList(0, i + 1));
            }
        }
        return null;
    }

    private List<String> segmentsUpTo(List<String> segments, String fullPath) {
        if (fullPath == null) return Collections.emptyList();
        List<String> parts = splitSegments(fullPath);
        // return the prefix segments in 'segments' that match the fullPath length
        int n = parts.size();
        if (n <= 0) return Collections.emptyList();
        return new ArrayList<>(segments.subList(0, Math.min(n, segments.size())));
    }

    // small helper holder
    private static final class RepeatAnalysis {
        final List<String> repeatableSegments;
        final String nearestRepeatAncestorFullPath;
        final String semanticRepeatAncestorPath;

        RepeatAnalysis(List<String> repeatableSegments, String nearestRepeatAncestorFullPath, String semanticRepeatAncestorPath) {
            this.repeatableSegments = repeatableSegments;
            this.nearestRepeatAncestorFullPath = nearestRepeatAncestorFullPath;
            this.semanticRepeatAncestorPath = semanticRepeatAncestorPath;
        }
    }
}
