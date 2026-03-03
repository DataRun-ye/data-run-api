package org.nmcpye.datarun.jpa.option;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;
import org.nmcpye.datarun.common.IdScheme;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObject;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An OptionSet.
 *
 * @author Hamza Assada
 * @since 10/09/2024
 */
@Entity
@Table(name = "option_set")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SQLDelete(sql = "UPDATE option_value SET deleted_at = now() WHERE id = ?")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OptionSet extends JpaIdentifiableObject {
    @Size(max = 11)
    @Column(name = "uid", length = 11, updatable = false, unique = true)
    protected String uid;

    /**
     * The unique code for this object.
     */
    @Column(name = "code", unique = true)
    protected String code;

    /**
     * The name of this object. Required and unique.
     */
    @Column(name = "name", nullable = false, unique = true)
    protected String name;

    @OneToMany(mappedBy = "optionSet", cascade = CascadeType.ALL,
        orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderColumn(name = "sort_order")
    @ListIndexBase(1)
    @Size(min = 1)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Option> options = new ArrayList<>();

    @Type(JsonType.class)
    @Column(name = "properties_map", columnDefinition = "jsonb")
    @JsonProperty
    protected Map<String, Object> properties;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void setDeleted(boolean deleted) {
        this.deletedAt = deleted ? Instant.now() : null;

        if (this.options != null) {
            for (Option opt : this.options) {
                if (opt != null) {
                    opt.setDeleted(deleted);
                }
            }
        }
    }

    @JsonProperty(value = "options")
    @JsonSerialize(contentAs = JpaIdentifiableObject.class)
    public List<Option> getOptions() {
        return options;
    }

    @JsonSetter(contentNulls = Nulls.SKIP)
    public void setOptions(List<Option> options) {
        getOptions().clear();
        getOptions().addAll(options);
        for (Option option : options) {
            option.setOptionSet(this);
        }
    }

    public void addOption(Option option) {
        this.options.add(option);
        option.setOptionSet(this);
    }

    @JsonIgnore
    public Set<String> getOptionCodesAsSet() {
        return options.stream().filter(Objects::nonNull).map(Option::getCode).collect(Collectors.toSet());
    }

    public Option getOptionByCode(String code) {
        for (Option option : options) {
            if (option != null && option.getCode().equals(code)) {
                return option;
            }
        }

        return null;
    }

    public Map<String, String> getOptionCodePropertyMap(IdScheme idScheme) {
        return options.stream().collect(Collectors.toMap(Option::getCode, o -> o.getPropertyValue(idScheme)));
    }

    public Option getOptionByUid(String uid) {
        for (Option option : options) {
            if (option != null && option.getUid().equals(uid)) {
                return option;
            }
        }

        return null;
    }
}
