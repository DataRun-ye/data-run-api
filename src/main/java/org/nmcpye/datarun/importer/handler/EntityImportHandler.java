package org.nmcpye.datarun.importer.handler;

import org.nmcpye.datarun.importer.dto.AbstractBaseDto;
import org.nmcpye.datarun.importer.service.ValidationContext;

import java.util.List;
import java.util.Map;

public interface EntityImportHandler<DTO extends AbstractBaseDto, Entity> {
    String getEntityName();            // "product"

    Class<DTO> getDtoClass();          // ProductDto.class

    void  validate(DTO dto, ValidationContext ctx, int rowIndex);

    Entity toEntity(DTO dto, Map<String, Object> resolvedRefs);

    void postProcess(Entity entity);

    void persistAll(List<Entity> entities, boolean dryRun);
}
//public interface EntityImportHandler<DTO, Entity> {
//
//    KeyType getDefaultKeyType();                       // e.g. UID for User, CODE for Product
//
//    String getEntityName();                  // e.g. "product"
//
//    Class<DTO> getDtoClass();                // used by JSON → DTO
//
//    void validate(DTO dto, ValidationContext ctx);
//
//    Entity toEntity(DTO dto, Map<String, Object> refs);
//
//    void postProcess(Entity entity);
//
//    void persistAll(List<Entity> entities, boolean dryRun);
//}
