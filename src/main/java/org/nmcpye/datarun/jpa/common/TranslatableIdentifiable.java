package org.nmcpye.datarun.jpa.common;

import com.fasterxml.jackson.annotation.*;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.common.translation.Translatable;
import org.nmcpye.datarun.common.translation.Translation;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.annotation.Transient;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada
 * @since 20/03/2025
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // tolerate extra fields in DB
@JsonInclude(JsonInclude.Include.NON_NULL)  // omit nulls when serializing
//@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
//    getterVisibility = JsonAutoDetect.Visibility.NONE,
//    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
abstract public class TranslatableIdentifiable extends JpaIdentifiableObject {

    /**
     * Set of available object translation, normally filtered by locale.
     */
    @Type(JsonType.class)
    @Column(name = "translations", columnDefinition = "jsonb")
    protected Set<Translation> translations = new HashSet<>();

    /**
     * Cache for object translations, where the cache key is a combination of
     * locale and translation property, and value is the translated value.
     */
    @JsonIgnore
    @Transient
    transient private Map<String, String> translationCache = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    // Comparable implementation
    // -------------------------------------------------------------------------

    @JsonIgnore
    @BsonIgnore
    public Map<String, String> getTranslationCache() {
        return translationCache;
    }


    // -------------------------------------------------------------------------
    // Setters and getters
    // -------------------------------------------------------------------------

    @JsonGetter("displayName")
    @Translatable(propertyName = "name", key = "name")
    public String getDisplayName() {
        return getTranslation("name", getName());
    }

    public Set<Translation> getTranslations() {
        if (translations == null) {
            translations = new HashSet<>();
        }

        return translations;
    }

    @JsonSetter("label")
    public JpaIdentifiableObject setLabel(Map<String, String> label) {
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

    /**
     * Clears out cache when setting translations.
     */
    public void setTranslations(Set<Translation> translations) {
        if (translations != null) {
            this.translationCache.clear();
            this.translations = translations;
        }
    }

    /**
     * Returns a translated value for this object for the given property. The
     * current locale is read from the user context.
     *
     * @param translationKey the translation key.
     * @param defaultValue   the value to use if there are no translations.
     * @return a translated value.
     */
    protected String getTranslation(String translationKey, String defaultValue) {
        String localeKey = SecurityUtils.getCurrentUserDetails()
            .map(CurrentUserDetails::getLangKey)
            .orElse(null);
        final String defaultTranslation = defaultValue != null ? defaultValue.trim() : null;

        if (localeKey == null || translationKey == null || CollectionUtils.isEmpty(translations)) {
            return defaultValue;
        }

        return translationCache.computeIfAbsent(Translation.getCacheKey(localeKey, translationKey),
            key -> getTranslationValue(localeKey, translationKey, defaultTranslation));
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

    protected String getTranslation(String translationKey, String localeKey, String defaultValue) {
        final String defaultTranslation = defaultValue != null ? defaultValue.trim() : null;

        if (localeKey == null || translationKey == null || CollectionUtils.isEmpty(translations)) {
            return defaultValue;
        }

        return translationCache.computeIfAbsent(Translation.getCacheKey(localeKey, translationKey),
            key -> getTranslationValue(localeKey, translationKey, defaultTranslation));
    }

    /**
     * Get Translation value from {@code Set<Translation>} by given locale and
     * translationKey
     *
     * @return Translation value if exists, otherwise return default value.
     */
    private String getTranslationValue(String locale, String translationKey, String defaultValue) {
        for (Translation translation : translations) {
            if (locale.equals(translation.getLocale()) && translationKey.equals(translation.getProperty()) &&
                !StringUtils.isEmpty(translation.getValue())) {
                return translation.getValue();
            }
        }

        return defaultValue;
    }
}
