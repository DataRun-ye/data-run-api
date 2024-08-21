package org.nmcpye.datarun.drun.postgres.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.domain.AbstractAuditingEntity;
import org.nmcpye.datarun.drun.postgres.common.translation.Translation;
import org.nmcpye.datarun.utils.CodeGenerator;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.nmcpye.datarun.drun.postgres.hibernate.HibernateProxyUtils.getRealClass;

@MappedSuperclass
public abstract class BaseIdentifiableObject<T> extends AbstractAuditingEntity<T> {


    @Type(JsonType.class)
    @Column(name = "translations", columnDefinition = "jsonb")
    protected Set<Translation> translations = new HashSet<>();

    /**
     * Cache for object translations, where the cache key is a combination of
     * locale and translation property, and value is the translated value.
     */
    protected transient Map<String, String> translationCache = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Comparable implementation
    // -------------------------------------------------------------------------

    /**
     * Compares objects based on display name. A null display name is ordered
     * after a non-null display name.
     */
    @Override
    public int compareTo(IdentifiableObject object) {
        if (this.getDisplayName() == null) {
            return object.getDisplayName() == null ? 0 : 1;
        }

        return object.getDisplayName() == null ? -1 : this.getDisplayName().compareToIgnoreCase(object.getDisplayName());
    }

    // -------------------------------------------------------------------------
    // Setters and getters
    // -------------------------------------------------------------------------

    //    public final static Class TYPE_OF_ID = Long.class; // When using id as aid
//    public final static Class TYPE_OF_ID = String.class; // when using uid as id

//    @Override
//    @JsonIgnore
//    @JsonProperty(value = "id")
//    @JacksonXmlProperty(localName = "id", isAttribute = true)
//    @Description("The Unique Identifier for this Object.")
//    @Property(value = PropertyType.IDENTIFIER, required = Property.Value.FALSE)
//    public T getId() {
//        return id;
//    }
//
//    public void setId(T id) {
//        this.id = id;
//    }
//
//    @Override
//    @JsonProperty(value = "id")
//    public String getUid() {
//        return uid;
//    }
//
//    public void setUid(String uid) {
//        this.uid = uid;
//    }
//
//    @Override
//    @JsonProperty
//    public String getCode() {
//        return code;
//    }
//
//    public void setCode(String code) {
//        this.code = code;
//    }
//
//    @Override
//    @JsonProperty
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }

    //    @Override
    //    @JsonProperty
    //    public String getDisplayName() {
    //                displayName = getTranslation( TranslationProperty.NAME, displayName );
    //        return displayName != null ? displayName : getName();
    //    }

    @Override
    @JsonProperty
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
        Locale locale = new Locale("ar");
//            CurrentUserUtil.getUserSetting(UserSettingKey.DB_LOCALE);

        final String defaultTranslation = defaultValue != null ? defaultValue.trim() : null;

        if (locale == null || translationKey == null || CollectionUtils.isEmpty(translations)) {
            return defaultValue;
        }

        return translationCache.computeIfAbsent(Translation.getCacheKey(locale.toString(), translationKey),
            key -> getTranslationValue(locale.toString(), translationKey, defaultTranslation));
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
    public boolean equals(Object obj) {
        return this == obj || obj instanceof BaseIdentifiableObject
            && getRealClass(this) == getRealClass(obj)
            && typedEquals((IdentifiableObject<T>) obj);
    }

    /**
     * Equality check against typed identifiable object. This method is not
     * vulnerable to proxy issues, where an uninitialized object class type
     * fails comparison to a real class.
     *
     * @param other the identifiable object to compare this object against.
     * @return true if equal.
     */
    public boolean typedEquals(IdentifiableObject<T> other) {
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
     * Set auto-generated fields on save or update
     */
    public void setAutoFields() {
        if (getUid() == null || getUid().length() == 0) {
            setUid(CodeGenerator.generateUid());
        }

        Instant date = Instant.now();
    }

    @Override
    public String toString() {
        return (
            "{" +
                "\"class\":\"" +
                getClass() +
                "\", " +
                "\"id\":\"" +
                getId() +
                "\", " +
                "\"uid\":\"" +
                getUid() +
                "\", " +
                "\"code\":\"" +
                getCode() +
                "\", " +
                "\"name\":\"" +
                getName() +
                "\", " +
                "\"created\":\"" +
                "\" " +
                "}"
        );
    }

    /**
     * Get Translation value from {@code Set<Translation>} by given locale and
     * translationKey
     *
     * @return Translation value if exists, otherwise return default value.
     */
    private String getTranslationValue(String locale, String translationKey, String defaultValue) {
        for (Translation translation : getTranslations()) {
            if (locale.equals(translation.getLocale()) && translationKey.equals(translation.getProperty()) &&
                !StringUtils.isEmpty(translation.getValue())) {
                return translation.getValue();
            }
        }

        return defaultValue;
    }
}
