//package org.nmcpye.datarun.config;
//
//import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
//import org.springframework.boot.autoconfigure.mongo.MongoProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.io.File;
//import java.io.IOException;
//
//@Configuration
//public class DebeziumConnectorConfig {
//
//    @Bean
//    public io.debezium.config.Configuration mongodbConnector(
//        MongoProperties mongoProperties,
//        DataSourceProperties dataSourceProperties) throws IOException {
//        File offsetStorageTempFile = File.createTempFile("offsets_", ".dat");
//        return io.debezium.config.Configuration.create()
//            // engine properties
//            .with("name", "sbd-mongodb")
//            .with("connector.class", "io.debezium.connector.mongodb.MongoDbConnector")
//
//            // File offset
//            .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
//            .with("offset.storage.file.filename", offsetStorageTempFile.getAbsolutePath())
//            .with("offset.flush.interval.ms", "60000")
//
//            // Jdbc Offset
////            .with("offset.storage", "io.debezium.storage.jdbc.offset.JdbcOffsetBackingStore")
////            .with("offset.storage.jdbc.url", dataSourceProperties.getUrl())
////            .with("offset.storage.jdbc.user", dataSourceProperties.getUsername())
////            .with("offset.storage.jdbc.password", dataSourceProperties.getPassword())
//
//            .with("mongodb.connection.string", mongoProperties.getUri())
////            .with("mongodb.dbname", "dataRunApi")
//            .with("topic.prefix", "sbd-mongodb-connector")
//            .with("collection.include.list", "dataRunApi.data_form_submission") // default empty
//            .with("errors.log.include.messages", "true")
//            .build();
//    }
//
//}
