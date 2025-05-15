//package org.nmcpye.datarun.mapper.dto;
//
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//import lombok.Getter;
//import lombok.Setter;
//import org.nmcpye.datarun.mongo.domain.dataelement.FormElementConf;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * DTO for {@link org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf}
// */
//@Getter
//@Setter
//public class FormSectionConfDto extends FormElementConf {
//    String id;
//    Boolean repeatable;
//    String path;
//    String parent;
//    String code;
//    @NotNull
//    String name;
//    @Size(max = 2000)
//    String description;
//    Map<String, String> label;
//    List<DataFieldRuleDto> rules;
//    Integer order;
//    List<String> appearance;
//}
