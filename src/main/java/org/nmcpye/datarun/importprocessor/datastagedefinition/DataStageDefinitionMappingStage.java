package org.nmcpye.datarun.importprocessor.datastagedefinition;

import org.nmcpye.datarun.importprocessor.ImportContext;
import org.nmcpye.datarun.importprocessor.ImportStage;
import org.nmcpye.datarun.jpa.datastage.dto.DataStageDefinitionDto;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada 02/06/2025 <7amza.it@gmail.com>
 */
@Component
public class DataStageDefinitionMappingStage implements ImportStage<DataStageDefinitionDto> {
    public void process(ImportContext<DataStageDefinitionDto> context) {
        for (DataStageDefinitionDto dto : context.rawData()) {
            // here you could do transformation if needed
            context.addProcessed(dto);
        }
    }
}
