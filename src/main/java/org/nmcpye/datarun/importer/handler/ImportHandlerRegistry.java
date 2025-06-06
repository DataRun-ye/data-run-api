//package org.nmcpye.datarun.importer.handler;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.ApplicationContext;
//import org.springframework.stereotype.Component;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * @author Hamza Assada 02/06/2025 <7amza.it@gmail.com>
// */
//@Component
//public class ImportHandlerRegistry {
//    private final Map<String, EntityImportHandler<?, ?>> handlers = new HashMap<>();
//
//    @Autowired
//    public ImportHandlerRegistry(ApplicationContext ctx) {
//        Map<String, EntityImportHandler> beans = ctx.getBeansOfType(EntityImportHandler.class);
//        for (EntityImportHandler<?, ?> handler : beans.values()) {
//            handlers.put(handler.getEntityName(), handler);
//        }
//    }
//
//    public EntityImportHandler<?, ?> getHandler(String entityName) {
//        return handlers.get(entityName);
//    }
//}
