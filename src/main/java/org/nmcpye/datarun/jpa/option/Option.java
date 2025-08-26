package org.nmcpye.datarun.jpa.option;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;

/**
 * @author Hamza Assada
 * @since 30/06/2025
 */
@Entity
@Table(name = "option_value", uniqueConstraints = {
        @UniqueConstraint(name = "uc_option_option_set_name", columnNames = {"name", "option_set_id"}),
        @UniqueConstraint(name = "uc_option_option_set_code", columnNames = {"code", "option_set_id"}),
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Option extends JpaBaseIdentifiableObject {
    /**
     * The unique code for this object.
     */
    @Column(name = "code", nullable = false, updatable = false)
    private String code;

    /**
     * The name of this object. Required and unique.
     */
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    // only getter, setting is done by jpa
    @Setter(value = AccessLevel.PRIVATE)
    @Column(name = "sort_order")
    private Integer sortOrder;

    @NotNull
    @ManyToOne
    // prevent JPA from trying to manage this FK twice
//    @JoinColumn(name = "option_set_id", insertable = false, updatable = false, nullable = false)
    @JoinColumn(name = "option_set_id", nullable = false, updatable = false)
    private OptionSet optionSet;

    @JsonProperty
    @JsonSerialize(as = JpaBaseIdentifiableObject.class)
    public OptionSet getOptionSet() {
        return optionSet;
    }

    @Override
    protected void setUid(String uid) {

    }

    @JsonIgnore
    @Override
    public String getUid() {
        return null;
    }
}
