package org.nmcpye.datarun.importer.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author Hamza Assada 03/06/2025 <7amza.it@gmail.com>
 */
@Accessors(fluent = true)
@Getter
@Setter
@ToString
public class ImportRequest {
    String entityType;
    String identifierKey;
    //    JsonNode node;
    Map<String, Object> data;
    ImportStrategy strategy;
    boolean dryRun;

    public enum ImportStrategy {CREATE, UPDATE, DELETE}
}
