//package org.nmcpye.datarun.drun.postgres.domain;
//
//import io.hypersistence.utils.hibernate.type.json.JsonType;
//import jakarta.persistence.*;
//import jakarta.validation.constraints.Size;
//import lombok.Getter;
//import lombok.Setter;
//import org.hibernate.annotations.Cache;
//import org.hibernate.annotations.CacheConcurrencyStrategy;
//import org.hibernate.annotations.Type;
//import org.nmcpye.datarun.drun.postgres.common.BaseIdentifiableObject;
//
//import java.util.Map;
//
///**
// * A DataOption.
// */
//@Entity
//@Table(name = "option_value")
//@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
//@Getter
//@Setter
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class OptionValue extends BaseIdentifiableObject<Long> {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
//    @SequenceGenerator(name = "sequenceGenerator")
//    @Column(name = "id")
//    private Long id;
//
//    @Size(max = 11)
//    @Column(name = "uid", length = 11, unique = true, nullable = false)
//    private String uid;
//
//    @Column(name = "code", unique = true)
//    private String code;
//
//    @Column(name = "name", unique = true, nullable = false)
//    private String name;
//
//    private Integer order;
//
//    private String filterExpression;
//
//    @Type(JsonType.class)
//    @Column(name = "label", columnDefinition = "jsonb")
//    private Map<String, String> label;
//
//    @Type(JsonType.class)
//    @Column(name = "label", columnDefinition = "jsonb")
//    private Map<String, Object> properties;
//}
