package org.nmcpye.datarun.jpa.flowtype;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.util.Objects;

/**
 * @author Hamza Assada 08/06/2025 <7amza.it@gmail.com>
 */
@JsonDeserialize(builder = FlowScopeType.Builder.class)
@Getter
public class FlowScopeType {
    private final String key;
    private final ScopePropertyType type;
    private final boolean required;
    private final boolean multiple;

    public boolean is(ScopePropertyType type) {
        return this.type == type;
    }

    /**
     * Only used and required for ENTITY type
     */
    private final String entityTypeId;

    // Private constructor (use builder)
    private FlowScopeType(ScopePropertyType type) {
        this.key = type.name();
        this.type = type;
        this.required = true;
        this.multiple = false;
        this.entityTypeId = null;
    }

    // Private constructor (use builder)
    private FlowScopeType(Builder builder, String entityTypeId) {
        this.key = builder.key;
        this.type = builder.type;
        this.required = builder.required;
        this.multiple = builder.multiple;
        this.entityTypeId = builder.type != ScopePropertyType.ENTITY ? null : entityTypeId;
    }
    // Getters (omitted for brevity)

    // Builder Class
    @SuppressWarnings("unused")
    public static class Builder {
        private String key;
        private ScopePropertyType type;
        private boolean required;
        private boolean multiple;
        private String entityTypeId;

        // Setters (return Builder for chaining)
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder type(ScopePropertyType type) {
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

        public FlowScopeType build() {
            if (type == null) {
                throw new IllegalArgumentException("Scope Type is required");
            }

            // Validate ENTITY constraint
            if (type == ScopePropertyType.ENTITY && (entityTypeId == null || entityTypeId.isEmpty())) {
                throw new IllegalArgumentException("entityTypeId is required for ENTITY scope type");
            }

            if (key == null || key.isEmpty()) {
                this.key(type.name());
            }

            return new FlowScopeType(this, entityTypeId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FlowScopeType that)) return false;
        return Objects.equals(getKey(), that.getKey()) && getType() == that.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getType());
    }
}
