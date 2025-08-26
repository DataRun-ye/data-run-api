//package org.nmcpye.datarun.config.datarun;
//
//import org.jooq.DSLContext;
//import org.jooq.SQLDialect;
//import org.jooq.impl.DSL;
//import org.jooq.impl.DefaultConfiguration;
//import org.springframework.beans.factory.ObjectProvider;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import javax.sql.DataSource;
//
///**
// * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
// */
//@Configuration
//public class JooqConfig {
//
//    // For more info on imports and data types, click on the "Help" icon
//// These imports, and possibly others, are implied:
//    @Bean
//    public DSLContext dslContext(ObjectProvider<DataSource> dataSource) {
//        var configuration = new DefaultConfiguration();
//        configuration.set(dataSource.getIfUnique());
//        configuration.set(SQLDialect.POSTGRES);
//        return DSL.using(configuration);
//    }
//}
