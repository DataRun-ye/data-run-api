package org.nmcpye.datarun.datatemplate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.datatemplate.DataTemplate;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.datatemplateversion.DataTemplateTemplateVersion;
import org.nmcpye.datarun.datatemplateversion.DataTemplateVersionInterface;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;

/**
 * DTO for {@link org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate}
 */
@Setter
@Getter
@AllArgsConstructor
public class DataTemplateInstanceDto implements DataTemplateVersionInterface, Serializable {
    /**
     * output the master {@link DataTemplate} uid
     */
    @Size(max = 11)
    private String uid;

    /**
     * {@link DataTemplateTemplateVersion} uid,
     * formVersion is treated as output-only (i.e. set in backend and ignored on inbound JSON)
     */
    @JsonProperty(access = READ_ONLY)
    private String versionUid;

    /**
     * currentVersion is treated as output-only (i.e. set in backend and ignored on inbound JSON)
     */
    @JsonProperty(access = READ_ONLY)
    private Integer versionNumber;

    // metadata (form Template)
    @NotNull
    @NotEmpty(message = "name cannot be empty")
    private String name;

    @Size(max = 2000)
    private String description;
    private Boolean deleted;
    private Map<String, String> label;
    //
    // versioned (form version)
    private List<FormDataElementConf> fields;
    private List<FormSectionConf> sections;
}
