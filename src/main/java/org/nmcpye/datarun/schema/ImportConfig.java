package org.nmcpye.datarun.schema;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Hamza Assada (01-05-2025), <7amza.it@gmail.com>
 */
@Configuration
public class ImportConfig {
    @Bean
    public ImportProcessor importProcessor(List<ImportableEntityHandler<?, ?>> handlers) {
        return new ImportProcessor(handlers);
    }
}
