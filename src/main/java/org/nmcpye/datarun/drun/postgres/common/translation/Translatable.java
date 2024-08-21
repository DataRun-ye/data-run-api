package org.nmcpye.datarun.drun.postgres.common.translation;

import jakarta.validation.constraints.NotNull;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used for indicating that a property of an object is
 * translatable. It must be applied to the getDisplay*() methods. See
 * {@link IdentifiableObject#getDisplayName()} for example.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Translatable {
    /**
     * Property name for enabling translation
     */
    @NotNull
    String propertyName();

    /**
     * Translation key for storing translation in json format. If not defined
     * then property name is used as the key.
     */
    String key() default "";
}
