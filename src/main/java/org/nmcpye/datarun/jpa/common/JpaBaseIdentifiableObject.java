package org.nmcpye.datarun.jpa.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import org.nmcpye.datarun.common.IdScheme;
import org.nmcpye.datarun.common.IdentifiableObject;
import org.nmcpye.datarun.common.IdentifiableProperty;
import org.nmcpye.datarun.common.translation.Translatable;
import org.nmcpye.datarun.common.translation.Translation;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.annotation.Transient;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada 20/03/2025 (7amza.it@gmail.com)
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
abstract public class JpaBaseIdentifiableObject extends JpaIdentifiableObject
    implements Comparable<JpaBaseIdentifiableObject> {

    /**
     * Set of available object translation, normally filtered by locale.
     */
    @JsonIgnore
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

    /**
     * Compares objects based on display name. A null display name is ordered
     * after a non-null display name.
     */
    @Override
    public int compareTo(JpaBaseIdentifiableObject object) {
        if (this.getDisplayName() == null) {
            return object.getDisplayName() == null ? 0 : 1;
        }

        return object.getDisplayName() == null ? -1
            : this.getDisplayName().compareToIgnoreCase(object.getDisplayName());
    }

    // -------------------------------------------------------------------------
    // Setters and getters
    // -------------------------------------------------------------------------

    @JsonProperty
    @Translatable(propertyName = "name", key = "NAME")
    public String getDisplayName() {
        return getTranslation("NAME", getName());
    }

    @JsonIgnore
    public Set<Translation> getTranslations() {
        if (translations == null) {
            translations = new HashSet<>();
        }

        return translations;
    }

    public void setLabel(Map<String, String> label) {
        // Create new name translations from the input map
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

    /**
     * Clears out cache when setting translations.
     */
    public void setTranslations(Set<Translation> translations) {
        this.translationCache.clear();
        this.translations = translations;
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
        String localeKey = SecurityUtils.getCurrentUserLocale().orElse("en");
        final String defaultTranslation = defaultValue != null ? defaultValue.trim() : null;

        if (localeKey == null || translationKey == null || CollectionUtils.isEmpty(translations)) {
            return defaultValue;
        }

        return translationCache.computeIfAbsent(Translation.getCacheKey(localeKey, translationKey),
            key -> getTranslationValue(localeKey, translationKey, defaultTranslation));
    }

    @JsonProperty
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

    // -------------------------------------------------------------------------
    // hashCode and equals
    // -------------------------------------------------------------------------

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);

        return result;
    }

    /**
     * Class check uses isAssignableFrom and get-methods to handle proxied
     * objects.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!getClass().isAssignableFrom(o.getClass())) {
            return false;
        }

        final JpaBaseIdentifiableObject other = (JpaBaseIdentifiableObject) o;

        if (getId() != null ? !getId().equals(other.getId()) : other.getId() != null) {
            return false;
        }

        if (getCode() != null ? !getCode().equals(other.getCode()) : other.getCode() != null) {
            return false;
        }

        if (getName() != null ? !getName().equals(other.getName()) : other.getName() != null) {
            return false;
        }

        return true;
    }

    /**
     * Equality check against typed identifiable object. This method is not
     * vulnerable to proxy issues, where an uninitialized object class type
     * fails comparison to a real class.
     *
     * @param other the identifiable object to compare this object against.
     * @return true if equal.
     */
    public boolean typedEquals(IdentifiableObject<?> other) {
        if (other == null) {
            return false;
        }

        if (getId() != null ? !getId().equals(other.getId()) : other.getId() != null) {
            return false;
        }

        if (getCode() != null ? !getCode().equals(other.getCode()) : other.getCode() != null) {
            return false;
        }

        if (getName() != null ? !getName().equals(other.getName()) : other.getName() != null) {
            return false;
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------
    @Override
    public String toString() {
        return "{" +
            "\"class\":\"" + getClass() + "\", " +
            "\"id\":\"" + getId() + "\", " +
            "\"code\":\"" + getCode() + "\", " +
            "\"name\":\"" + getName() + "\", " +
            "\"createdDate\":\"" + getCreatedDate() + "\", " +
            "\"lastModifiedDate\":\"" + getLastModifiedDate() + "\" " +
            "}";
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

    @Override
    public String getPropertyValue(IdScheme idScheme) {
        if (idScheme.isNull() || idScheme.is(IdentifiableProperty.ID)) {
            return getId();
        } else if (idScheme.is(IdentifiableProperty.CODE)) {
            return getCode();
        } else if (idScheme.is(IdentifiableProperty.NAME)) {
            return getName();
        }

        return null;
    }

}
