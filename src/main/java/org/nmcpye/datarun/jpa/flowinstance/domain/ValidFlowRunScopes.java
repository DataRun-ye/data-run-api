package org.nmcpye.datarun.jpa.flowinstance.domain;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Hamza Assada 09/06/2025 <7amza.it@gmail.com>
 */
@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = FlowRunScopesValidator.class)
public @interface ValidFlowRunScopes {

    String message() default "Invalid scopes";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
