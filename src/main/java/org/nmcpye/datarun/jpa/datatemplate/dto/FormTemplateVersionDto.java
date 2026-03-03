package org.nmcpye.datarun.jpa.datatemplate.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;

import java.io.Serializable;
import java.util.List;

@Value
public class FormTemplateVersionDto implements Serializable {
    @Size(max = 26)
    String id;
    String code;
    @Size(max = 11)
    String uid;
    @Size(max = 11)
    @NotNull
    String templateUid;
    Integer versionNumber;
    List<FormDataElementConf> fields;
    List<FormSectionConf> sections;
}
