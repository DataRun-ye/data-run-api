package org.nmcpye.datarun.jpa.optionset;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.datatemplateelement.DataOption;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;

import java.util.List;

/**
 * An OptionSet.
 *
 * @author Hamza Assada 10/09/2024 <7amza.it@gmail.com>
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
    private List<DataOption> options;

//    @OneToMany(cascade = CascadeType.ALL)
//    @JoinColumn(name = "option_set_id")
//    @OrderColumn(name = "sort_order")
//    @ListIndexBase(1)
//    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
//    private List<Option> optionSetOptions = new ArrayList<>();
}
