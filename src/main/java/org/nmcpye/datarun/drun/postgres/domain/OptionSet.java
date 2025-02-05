package org.nmcpye.datarun.drun.postgres.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.drun.postgres.common.BaseIdentifiableObject;
import org.nmcpye.datarun.mongo.domain.DataOption;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A OuLevel.
 */
@Entity
@Table(name = "option_set")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OptionSet extends BaseIdentifiableObject<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, unique = true, nullable = false)
    private String uid;

    @Column(name = "code", unique = true)
    private String code;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Type(JsonType.class)
    @Column(name = "options", columnDefinition = "jsonb")
    private List<DataOption> options = new ArrayList<>();

    public OptionSet() {
        setAutoFields();
    }

    public Long getId() {
        return this.id;
    }

    public OptionSet id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public OptionSet uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public OptionSet name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DataOption> getOptions() {
        return options;
    }

    public void setOptions(List<DataOption> options) {
        this.options = options;
    }
}
