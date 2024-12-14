package org.nmcpye.datarun.drun.postgres.common.translation;

import jakarta.validation.constraints.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
