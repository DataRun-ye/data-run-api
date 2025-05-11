package org.nmcpye.datarun.dto.formtemplate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;
import org.nmcpye.datarun.dto.formelement.FormDataElementConfDto;
import org.nmcpye.datarun.dto.formelement.FormSectionConfDto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * DTO for {@link org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate}
 */
@Value
public class DataFormTemplateVersionDto implements Serializable {
//    @Size(max = 11)
//    String versionId;

    @Size(max = 11)
    String uid;

    @NotNull
    String name;

    @NotNull
    String templateUid;

    @Size(max = 2000)
    String description;

    Integer version;

    String defaultLocale;

    Map<String, String> label;

    List<FormDataElementConfDto> fields;

    List<FormSectionConfDto> sections;

}
