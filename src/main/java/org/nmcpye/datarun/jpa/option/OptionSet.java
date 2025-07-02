package org.nmcpye.datarun.jpa.option;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.common.IdScheme;
import org.nmcpye.datarun.datatemplateelement.DataOption;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An OptionSet.
 *
 * @author Hamza Assada 10/09/2024 (7amza.it@gmail.com)
 */
@Entity
@Table(name = "option_set")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OptionSet extends JpaBaseIdentifiableObject {
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

    @Type(JsonType.class)
    @Column(name = "options", columnDefinition = "jsonb")
    private List<DataOption> legacyOptions;

    @OneToMany(mappedBy = "optionSet", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderColumn(name = "sort_order")
    @ListIndexBase(1)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Option> options = new ArrayList<>();

    @JsonIgnore
    public List<DataOption> getLegacyOptions() {
        return legacyOptions;
    }

    @JsonProperty(value = "options")
    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
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
//    public void addOption(Option option) {
//        if (option.getSortOrder() == null) {
//            this.options.add(option);
//        } else {
//            boolean added = false;
//            final int size = this.options.size();
//            for (int i = 0; i < size; i++) {
//                Option thisOption = this.options.get(i);
//                if (thisOption.getSortOrder() == null || thisOption.getSortOrder() > option.getSortOrder()) {
//                    this.options.add(i, option);
//                    added = true;
//                    break;
//                }
//            }
//            if (!added) {
//                this.options.add(option);
//            }
//        }
//        option.setOptionSet(this);
//    }

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
