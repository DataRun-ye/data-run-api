//package org.nmcpye.datarun.analytics.projection.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.dao.EmptyResultDataAccessException;
//import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.stream.Collectors;
//
///**
// * PilotRunner
// * <p>
// * Simple, robust runner to execute the pilot: backfill raw_repeat_payload for one or more repeat_uids,
// * then run the projection for those repeat_uids.
// * <p>
// * How to use:
// * - Configure defaults in application.properties (pilot.repeat.uids, pilot.page.size, pilot.run.backfill, pilot.run.projection)
// * - Or pass repeat uids as CLI args: e.g.
// * java -jar app.jar RPT_aaf9a7ade8ad8535 RPT_0cb69bd7ed22c6d5
// * - Or run with Spring Boot maven plugin and pass arguments:
// * mvn spring-boot:run -Dspring-boot.run.arguments="--pilot.repeat.uids=RPT_aaf9a7..."
// *
// * @author Hamza Assada
// * @since 18/09/2025
// */
//@Component
//public class PilotRunner implements CommandLineRunner {
//    private static final Logger log = LoggerFactory.getLogger(PilotRunner.class);
//
//    private final RawRepeatBackfill repeatBackfiller;
//    private final ProjectionService projectionService;
//    private final NamedParameterJdbcTemplate jdbc;
//
//    @Value("${pilot.repeat.uids:}")
//    private String pilotRepeatUidsProperty; // comma separated default
//
//    @Value("${pilot.page.size:500}")
//    private int pageSize;
//
//    @Value("${pilot.run.backfill:true}")
//    private boolean runBackfill;
//
//    @Value("${pilot.run.projection:true}")
//    private boolean runProjection;
//
//    public PilotRunner(RawRepeatBackfill repeatBackfiller,
//                       ProjectionService projectionService,
//                       NamedParameterJdbcTemplate jdbc) {
//        this.repeatBackfiller = repeatBackfiller;
//        this.projectionService = projectionService;
//        this.jdbc = jdbc;
//    }
//
////    @Override
////    public void run(String... args) throws Exception {
////        List<String> repeats = resolveRepeatUids(args);
////        if (repeats.isEmpty()) {
////            log.info("No repeat uids provided (args or pilot.repeat.uids). PilotRunner will exit.");
////            return;
////        }
////
////        log.info("PilotRunner starting for repeats: {} | pageSize={} | runBackfill={} | runProjection={}", repeats, pageSize, runBackfill, runProjection);
////
////        for (String repeatUid : repeats) {
////            log.info("--- START repeatUid={} ---", repeatUid);
////            long t0 = System.currentTimeMillis();
////            try {
////                if (runBackfill) {
////                    log.info("Running backfill for {} (pageSize={})...", repeatUid, pageSize);
////                    repeatBackfiller.backfillRepeat(repeatUid, pageSize);
////                    long c = queryRawCount(repeatUid);
////                    log.info("Backfill finished for {} — raw_repeat_payload rows now: {}", repeatUid, c);
////                }
////
////                if (runProjection) {
////                    log.info("Running projection for {}...", repeatUid);
////                    projectionService.runForRepeat(repeatUid);
////                    log.info("Projection finished for {}.", repeatUid);
////                }
////
////                long elapsed = System.currentTimeMillis() - t0;
////                log.info("--- DONE repeatUid={} ({} ms) ---", repeatUid, elapsed);
////            } catch (Exception ex) {
////                log.error("Error processing repeatUid={}: {}", repeatUid, ex.getMessage(), ex);
////            }
////        }
////
////        log.info("PilotRunner finished for all repeats.");
////    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        List<String> repeatsToRun = resolveRepeatUids(args);
//        // ... the list of repeatUids you want to process (passed earlier)
//        for (String repeatUid : repeatsToRun) {
//            log.info("--- START repeatUid={} ---", repeatUid);
//
//            // 1) try to load semantic paths from projection_config
//            List<String> semanticPaths = loadSemanticPathsFromProjectionConfig(repeatUid);
//
//            // 2) fallback: discover from template_element
//            if (semanticPaths.isEmpty()) {
//                semanticPaths = discoverSemanticPathsForRepeat(repeatUid);
//            }
//
//            // 3) call service to process repeat across all semantic paths (pageSize configurable)
//            int pageSize = 500; // or get from app config
//            projectionService.processRepeatForPaths(repeatUid, semanticPaths, pageSize);
//
//            log.info("--- DONE repeatUid={} ---", repeatUid);
//        }
//    }
//
//    private List<String> resolveRepeatUids(String[] args) {
//        if (args != null && args.length > 0) {
//            return Arrays.stream(args)
//                .filter(s -> s != null && !s.trim().isEmpty())
//                .collect(Collectors.toList());
//        }
//        if (pilotRepeatUidsProperty != null && !pilotRepeatUidsProperty.trim().isEmpty()) {
//            return Arrays.stream(pilotRepeatUidsProperty.split(","))
//                .map(String::trim)
//                .filter(s -> !s.isEmpty())
//                .collect(Collectors.toList());
//        }
//        return List.of();
//    }
//
//    private long queryRawCount(String repeatUid) {
//        try {
//            String sql = "SELECT count(*) FROM raw_repeat_payload WHERE repeat_uid = :r";
//            var params = java.util.Collections.singletonMap("r", repeatUid);
//            Number n = jdbc.queryForObject(sql, params, Number.class);
//            return n == null ? 0L : n.longValue();
//        } catch (Exception ex) {
//            log.warn("Unable to query raw_repeat_payload count for {}: {}", repeatUid, ex.getMessage());
//            return -1L;
//        }
//    }
//
//    /**
//     * try projection_config first
//     */
//    private List<String> loadSemanticPathsFromProjectionConfig(String repeatUid) {
//        try {
//            String sql = "SELECT payload->'semantic_paths' FROM projection_config WHERE source_repeat_uid = :repeatUid LIMIT 1";
//            MapSqlParameterSource p = new MapSqlParameterSource().addValue("repeatUid", repeatUid);
//            String jsonArrStr = jdbc.queryForObject(sql, p, String.class);
//            if (jsonArrStr == null) return Collections.emptyList();
//            JsonNode arr = new ObjectMapper().readTree(jsonArrStr);
//            if (!arr.isArray()) return Collections.emptyList();
//            List<String> out = new ArrayList<>();
//            arr.forEach(n -> out.add(n.asText()));
//            return out;
//        } catch (EmptyResultDataAccessException x) {
//            return Collections.emptyList();
//        } catch (Exception e) {
//            log.warn("Failed to load semantic_paths from projection_config for {}: {}", repeatUid, e.getMessage());
//            return Collections.emptyList();
//        }
//    }
//
//    /**
//     * discover from template_element table
//     */
//    private List<String> discoverSemanticPathsForRepeat(String repeatUid) {
//        String sql = "SELECT DISTINCT semantic_path FROM template_element WHERE repeat_uid = :repeatUid";
//        MapSqlParameterSource p = new MapSqlParameterSource().addValue("repeatUid", repeatUid);
//        List<String> paths = jdbc.queryForList(sql, p, String.class);
//        return paths;
//    }
//}
