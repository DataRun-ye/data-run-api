//package org.nmcpye.datarun.option;
//
//import jakarta.persistence.*;
//import jakarta.validation.constraints.Size;
//import lombok.Getter;
//import lombok.Setter;
//import org.hibernate.annotations.Cache;
//import org.hibernate.annotations.CacheConcurrencyStrategy;
//import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
//import org.nmcpye.datarun.optionset.OptionSet;
//
///**
// * A template Element Option.
// */
//@Entity
//@Table(name = "option_value")
//@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
//@Getter
//@Setter
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class Option extends
//    JpaBaseIdentifiableObject {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
//    @SequenceGenerator(name = "sequenceGenerator")
//    @Column(name = "id")
//    protected Long id;
//
//    @Size(max = 11)
//    @Column(name = "uid", length = 11, nullable = false, unique = true)
//    protected String uid;
//
//    /**
//     * The unique code for this object.
//     */
//    @Column(name = "code", unique = true)
//    protected String code;
//
//    /**
//     * The name of this object. Required and unique.
//     */
//    @Column(name = "name", nullable = false, unique = true)
//    protected String name;
//
//
//    @Column(name = "description")
//    private String description;
//
//    @Column(name = "sort_order")
//    private Integer sortOrder;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "id")
//    private OptionSet optionSet;
//}
