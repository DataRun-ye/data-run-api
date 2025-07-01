//package org.nmcpye.datarun.importer.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.nmcpye.datarun.importer.dto.AbstractBaseDto;
//import org.nmcpye.datarun.importer.exception.DryRunException;
//import org.nmcpye.datarun.importer.handler.EntityImportHandler;
//import org.nmcpye.datarun.importer.handler.ImportHandlerRegistry;
//import org.nmcpye.datarun.importer.util.ImportResponse;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
///**
// * @author Hamza Assada 02/06/2025 (7amza.it@gmail.com)
// */
//@Service
//public class ImportService {
//    private final ImportHandlerRegistry handlerRegistry;
//    private final ObjectMapper objectMapper;
//
//    @Autowired
//    public ImportService(ImportHandlerRegistry handlerRegistry, ObjectMapper objectMapper) {
//        this.handlerRegistry = handlerRegistry;
//        this.objectMapper = objectMapper;
//    }
//
//    @Transactional
//    public ImportResponse handleImport(String entityType, List<JsonNode> rawRows, boolean dryRun) {
//        EntityImportHandler<? extends AbstractBaseDto, ?> rawHandler = handlerRegistry.getHandler(entityType);
//        if (rawHandler == null) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown entityType '" + entityType + "'");
//        }
//
//        var dtos = parsePayload(rawRows, rawHandler.getDtoClass());
//
//
//        EntityImportHandler<? extends AbstractBaseDto, ?> handler = rawHandler;
//
//
//        // 2) Validate
//        ValidationContext ctx = new ValidationContext();
//        for (int i = 0; i < dtos.size(); i++) {
//            var dto = dtos.get(i);
//            handler.validate(dto, ctx, i);
//        }
//        if (ctx.hasErrors()) {
//            return ImportResponse.failure(ctx.getErrors(), dtos.size());
//        }
//
//        // 3) Convert DTOs → Entities
//        List<Object> entities = new ArrayList<>();
//        for (Object dto : dtos) {
//            Object entity = handler.toEntity(dto, Collections.emptyMap());
//            handler.postProcess(entity);
//            entities.add(entity);
//        }
//
//        // 4) Persist or simulate
//        handler.persistAll(entities, dryRun);
//
//        // 5) If dryRun, force rollback and return success payload
//        if (dryRun) {
//            throw new DryRunException("Dry run successful, rolling back transaction");
//        }
//
//        return ImportResponse.success(false, dtos.size());
//    }
//
//    private <D extends AbstractBaseDto> List<D> parsePayload(List<JsonNode> rawRows, Class<D> dtoClass) {
//        // 1) Parse rawRows → DTOs
//        List<D> dtos = new ArrayList<>();
//        for (int i = 0; i < rawRows.size(); i++) {
//            JsonNode node = rawRows.get(i);
//            D dto;
//            try {
//                dto = objectMapper.treeToValue(node, dtoClass);
//            } catch (Exception ex) {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse row " + i + ": " + ex.getMessage());
//            }
//            dtos.add(dto);
//        }
//    }
//}
