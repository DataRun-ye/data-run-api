package org.nmcpye.datarun.datatemplateversion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate}
 */
@Value
@Getter
@Setter
public class FormTemplateVersionDto implements Serializable {
    @NotNull
    String uid;
    @Size(max = 11)
    @NotNull
    String templateUid;
    Integer versionNumber;
    List<FormDataElementConf> fields;
    List<FormSectionConf> sections;
}
