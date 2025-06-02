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

/**
 * @author Hamza Assada, 20/03/2025
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
abstract public class JpaBaseIdentifiableObject
    extends JpaAuditableObject implements IdentifiableObject<Long> {

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

    public JpaBaseIdentifiableObject(JpaBaseIdentifiableObject identifiableObject) {
        this.setId(identifiableObject.getId());
        this.setUid(identifiableObject.getUid());
        this.setName(identifiableObject.getName());
        this.translations = identifiableObject.getTranslations();
    }
    // -------------------------------------------------------------------------
    // Comparable implementation
    // -------------------------------------------------------------------------

    @JsonIgnore
    @BsonIgnore
    public Map<String, String> getTranslationCache() {
        return translationCache;
    }

    public abstract String getName();

    public abstract String getCode();

    public abstract void setName(String name);

    public abstract void setCode(String code);

    /**
     * Compares objects based on display name. A null display name is ordered
     * after a non-null display name.
     */
    @Override
    public int compareTo(IdentifiableObject object) {
        if (this.getDisplayName() == null) {
            return object.getDisplayName() == null ? 0 : 1;
        }

        return object.getDisplayName() == null ? -1
            : this.getDisplayName().compareToIgnoreCase(object.getDisplayName());
    }

    // -------------------------------------------------------------------------
    // Setters and getters
    // -------------------------------------------------------------------------

    @Override
    @JsonProperty
    @Translatable(propertyName = "name", key = "NAME")
    public String getDisplayName() {
        return getTranslation("NAME", getName());
    }

    @Override
    @JsonProperty
    public Set<Translation> getTranslations() {
        if (translations == null) {
            translations = new HashSet<>();
        }

        return translations;
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
        String localeKey = SecurityUtils.getCurrentUserLocale().orElse(null);
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
        int result = getUid() != null ? getUid().hashCode() : 0;
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

        if (getUid() != null ? !getUid().equals(other.getUid()) : other.getUid() != null) {
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

        if (getUid() != null ? !getUid().equals(other.getUid()) : other.getUid() != null) {
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

    /**
     * Returns the value of the property referred to by the given IdScheme.
     *
     * @param idScheme the IdScheme.
     * @return the value of the property referred to by the IdScheme.
     */
    @Override
    public String getPropertyValue(IdScheme idScheme) {
        if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
            return getUid();
        } else if (idScheme.is(IdentifiableProperty.CODE)) {
            return getCode();
        } else if (idScheme.is(IdentifiableProperty.NAME)) {
            return getName();
        }

        return null;
    }

    @Override
    public String toString() {
        return "{" +
            "\"class\":\"" + getClass() + "\", " +
            "\"id\":\"" + getId() + "\", " +
            "\"uid\":\"" + getUid() + "\", " +
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

}
