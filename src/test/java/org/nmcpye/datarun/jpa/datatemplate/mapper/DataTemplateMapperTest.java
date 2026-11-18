package org.nmcpye.datarun.jpa.datatemplate.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateDto;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.jpa.datatemplate.dto.FormTemplateVersionDto;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DataTemplateMapperTest {

    private final DataTemplateMapper mapper = Mappers.getMapper(DataTemplateMapper.class);

    @Test
    void instanceOfAnOlderVersionKeepsThatVersionsIdentity() {
        DataTemplateDto template = new DataTemplateDto(
            "Tcf3Ks9ZRpB", "TLpAhIscGLm", 31, false, "ITN households", null, Map.of());
        FormTemplateVersionDto version30 = new FormTemplateVersionDto(
            "cz06mCos84F", "Tcf3Ks9ZRpB", 30, List.of(), List.of());

        DataTemplateInstanceDto instance = mapper.toInstanceDto(template, version30);

        assertThat(instance.getUid()).isEqualTo("Tcf3Ks9ZRpB");
        assertThat(instance.getName()).isEqualTo("ITN households");
        assertThat(instance.getVersionUid()).isEqualTo("cz06mCos84F");
        assertThat(instance.getVersionNumber()).isEqualTo(30);
    }
}
