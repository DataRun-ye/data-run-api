package org.nmcpye.datarun.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DebeziumConnectorConfig {

    @Bean
    public io.debezium.config.Configuration mongodbConnector(
        MongoProperties mongoProperties,
        DataSourceProperties dataSourceProperties) {

        return io.debezium.config.Configuration.create()
            // engine properties
            .with("name", "sbd-mongodb")
            .with("connector.class", "io.debezium.connector.mongodb.MongoDbConnector")

            .with("offset.storage", "io.debezium.storage.jdbc.offset.JdbcOffsetBackingStore")
            .with("offset.storage.jdbc.url", dataSourceProperties.getUrl())
            .with("offset.storage.jdbc.user", dataSourceProperties.getUsername())
            .with("offset.storage.jdbc.password", dataSourceProperties.getPassword())

            .with("mongodb.connection.string", mongoProperties.getUri())
            .with("topic.prefix", "sbd-mongodb-connector")
            .with("collection.include.list", "data_form_submission") // default empty
            .with("errors.log.include.messages", "true")
            .build();
    }

}
