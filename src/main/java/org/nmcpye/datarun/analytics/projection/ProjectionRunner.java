//package org.nmcpye.datarun.analytics.projection;
//
//import org.springframework.boot.CommandLineRunner;
//
///**
// * @author Hamza Assada
// * @since 17/09/2025
// */
////@Component
//public class ProjectionRunner implements CommandLineRunner {
//    private final ProjectionService projectionService;
//    public ProjectionRunner(ProjectionService projectionService) {
//        this.projectionService = projectionService;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        // Simple: run pilot projection for projection config UID passed as arg or default
//        String cfg = args.length > 0 ? args[0] : "proj_order_lines_v1";
//        projectionService.projectOrderLinesForRepeat(cfg);
//        System.out.println("Projection run finished for " + cfg);
//    }
//}
