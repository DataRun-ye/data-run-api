package org.nmcpye.datarun.jpa.scopeinstance;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.util.Objects;

/**
 * @author Hamza Assada 08/06/2025 <7amza.it@gmail.com>
 */
@JsonDeserialize(builder = ScopeElement.Builder.class)
@Getter
public class ScopeElement {
    private final String key;
    private final CoreElementType type;
    private final boolean required;
    private final boolean multiple;

    /**
     * Only used and required for ENTITY type
     */
    private final String entityTypeId;

    public boolean is(CoreElementType type) {
        return this.type == type;
    }


    // Private constructor (use builder)
    private ScopeElement(CoreElementType type) {
        this.key = type.name();
        this.type = type;
        this.required = true;
        this.multiple = false;
        this.entityTypeId = null;
    }

    // Private constructor (use builder)
    private ScopeElement(Builder builder, String entityTypeId) {
        this.key = builder.key;
        this.type = builder.type;
        this.required = builder.required;
        this.multiple = builder.multiple;
        this.entityTypeId = builder.type != CoreElementType.ENTITY ? null : entityTypeId;
    }

    // Getters (omitted for brevity)

    // Builder Class
    @SuppressWarnings("unused")
    public static class Builder {
        private String key;
        private CoreElementType type;
        private boolean required;
        private boolean multiple;
        private String entityTypeId;

        // Setters (return Builder for chaining)
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder type(CoreElementType type) {
            this.type = type;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder multiple(boolean multiple) {
            this.multiple = multiple;
            return this;
        }

        public Builder entityTypeId(String entityTypeId) {
            this.entityTypeId = entityTypeId;
            return this;
        }

        public ScopeElement build() {
            if (type == null) {
                throw new IllegalArgumentException("Scope Type is required");
            }

            // Validate ENTITY constraint
            if (type == CoreElementType.ENTITY && (entityTypeId == null || entityTypeId.isEmpty())) {
                throw new IllegalArgumentException("entityTypeId is required for ENTITY scope type");
            }

            if (key == null || key.isEmpty()) {
                this.key(type.name());
            }

            return new ScopeElement(this, entityTypeId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ScopeElement that)) return false;
        return Objects.equals(getKey(), that.getKey()) && getType() == that.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getType());
    }
}
