package org.nmcpye.datarun.mapper.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.mongo.common.FormWithFields;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.wildfly.common.annotation.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * DTO for {@link org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate}
 */
@Setter
@Getter
@AllArgsConstructor
public class SaveFormTemplateDto implements FormWithFields, Serializable {
    @Size(max = 11)
    String uid;

    @NotNull
    String name;

    @Size(max = 2000)
    String description;

    Boolean disabled;

    Boolean deleted;

    String defaultLocale;

    Map<String, String> label;

    List<FormDataElementConf> fields;
    List<FormSectionConf> sections;
}
