//package org.nmcpye.datarun.web.rest.v1.pivot;
//
//import org.nmcpye.datarun.jpa.pivotdata.dto.DimensionDto;
//import org.nmcpye.datarun.jpa.pivotdata.dto.MeasureDto;
//import org.nmcpye.datarun.jpa.pivotdata.dto.PivotMetadataResponse;
//import org.nmcpye.datarun.jpa.pivotdata.query.CardinalityService;
//import org.nmcpye.datarun.jpa.pivotdata.query.PivotRegistry;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.stream.Collectors;
//
///**
// * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
// */
//@RestController
//public class PivotMetadataController {
//
//    private final PivotRegistry registry;
//    private final CardinalityService cardinalityService;
//    private final long maxCells = 50_000L;
//    private final int maxCols = 200;
//
//    public PivotMetadataController(
//        PivotRegistry registry, CardinalityService cardinalityService) {
//        this.registry = registry;
//        this.cardinalityService = cardinalityService;
//    }
//
//    @GetMapping("/api/pivot/metadata")
//    public PivotMetadataResponse getMetadata(@RequestParam(required = false) String templateId) {
//        var dims = registry.listDimensions(templateId).stream().map(d -> {
//            Long hint = cardinalityService.getCardinalityHint(d.getId(), templateId);
//            return DimensionDto.from(d, hint);
//        }).collect(Collectors.toList());
//
//        var measures = registry.listMeasures(templateId)
//            .stream().map(MeasureDto::from).collect(Collectors.toList());
//
//        var config = PivotMetadataResponse.ConfigDto.from(maxCells, maxCols, 10_000); // default timeout ms
//
//        return new PivotMetadataResponse(dims, measures, config);
//    }
//}
