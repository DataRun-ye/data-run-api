//package org.nmcpye.datarun.config;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import org.javers.core.ChangesByCommit;
//import org.javers.core.diff.Change;
//import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
//
//import java.util.List;
//
///**
// * @author Hamza Assada 18/07/2025 (7amza.it@gmail.com)
// */
//@Configuration
//public class JaVersConfiguration {
//
//    @Bean
//    Jackson2ObjectMapperBuilderCustomizer jacksonJaVersCustomizer() {
//        return (Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder) ->
//            jacksonObjectMapperBuilder
//                .mixIn(ChangesByCommit.class, JaVersChangesMixIn.class);
//    }
//
//    /**
//     * Makes `changes` returned by non-standard `get()` method serializable by Jackson.
//     */
//    abstract static class JaVersChangesMixIn {
//        @JsonProperty("changes")
//        abstract List<Change> get();
//    }
//}
