package org.nmcpye.datarun.mongo.datatemplateversion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate}
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class FormTemplateVersionDto extends BaseDto implements Serializable {
    @Size(max = 11)
    String uid;
    @Size(max = 11)
    @NotNull
    String templateUid;
    Integer versionNumber;
    List<FormDataElementConf> fields;
    List<FormSectionConf> sections;
}
