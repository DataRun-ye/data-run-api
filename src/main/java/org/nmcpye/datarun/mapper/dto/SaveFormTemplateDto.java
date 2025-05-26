package org.nmcpye.datarun.mapper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.mongo.common.FormWithFields;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplate;
import org.nmcpye.datarun.mongo.domain.dataform.FormTemplateVersion;

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
public class SaveFormTemplateDto implements FormWithFields, Serializable {
    /**
     * output the master {@link FormTemplate} uid
     */
    @Size(max = 11)
    private String uid;

    /**
     * {@link FormTemplateVersion} uid,
     * formVersion is treated as output-only (i.e. set in backend and ignored on inbound JSON)
     */
    @JsonProperty(access = READ_ONLY)
    private String formVersion;

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
    private Boolean disabled;
    private Boolean deleted;
    private String defaultLocale;
    private Map<String, String> label;
    //
    // versioned (form version)
    private List<FormDataElementConf> fields;
    private List<FormSectionConf> sections;
}
