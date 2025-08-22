//package org.nmcpye.datarun.jpa.pivotdata.query;
//
//import org.nmcpye.datarun.jpa.pivotdata.model.DimensionDefinition;
//import org.nmcpye.datarun.jpa.pivotdata.model.MeasureDefinition;
//
//import java.util.List;
//import java.util.Optional;
//
///**
// * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
// */
//public interface PivotRegistry {
//    Optional<DimensionDefinition> getDimension(String id);
//
//    Optional<MeasureDefinition> getMeasure(String id);
//
//    List<DimensionDefinition> listDimensions(String templateId); // templateId optional filter
//
//    List<MeasureDefinition> listMeasures(String templateId);
//
//    void registerDimension(DimensionDefinition def, String templateId);
//
//    void registerMeasure(MeasureDefinition def, String templateId);
//}
