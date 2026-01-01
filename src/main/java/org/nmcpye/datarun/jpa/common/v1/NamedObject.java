package org.nmcpye.datarun.jpa.common.v1;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.translation.Translatable;
import org.nmcpye.datarun.common.translation.Translation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada
 * @since 20/03/2025
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("unused")
abstract public class NamedObject extends TranslatableObject {
    // -------------------------------------------------------------------------
    // Setters and getters
    // -------------------------------------------------------------------------

    public abstract String getName();

    @JsonGetter("displayName")
    @Translatable(propertyName = "name", key = "name")
    public String getDisplayName() {
        return getTranslation("name", getName());
    }

    @JsonSetter("label")
    public NamedObject setLabel(Map<String, String> label) {
        // Create new name translations from the input map
        if (label != null) {
            Set<Translation> newNameTranslations = label.entrySet().stream()
                .map(entry -> Translation.builder()
                    .locale(entry.getKey())
                    .property("name")
                    .value(entry.getValue())
                    .build())
                .collect(Collectors.toSet());
            // Create updated translation set:
            // 1. Preserve existing non-name translations
            // 2. Add/replace name translations from new map
            Set<Translation> updatedTranslations = getTranslations().stream()
                .filter(t -> !"name".equals(t.getProperty())) // Keep non-name translations
                .collect(Collectors.toCollection(HashSet::new));

            updatedTranslations.addAll(newNameTranslations); // Merge new name translations

            setTranslations(updatedTranslations);
        }
        return this;
    }

    @Transient
    public Map<String, String> getLabel() {
        if (translations == null || translations.isEmpty()) {
            return null;
        }
        return Map.ofEntries(
            Map.entry("ar", getTranslation("name", "ar", getName())),
            Map.entry("en", getTranslation("name", "en", getName()))
        );
    }
}
