package org.nmcpye.datarun.jpa.pivotdata.query;

import org.nmcpye.datarun.jpa.pivotdata.model.DimensionDefinition;
import org.nmcpye.datarun.jpa.pivotdata.model.MeasureDefinition;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Small in-memory registry for Phase 0. Stores global dims/measures and per-template overrides.
 * Replace/extend when wiring to config DB or codegen (jOOQ).
 *
 * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
 */
@Service
public class InMemoryPivotRegistry implements PivotRegistry {

    // global id -> def
    private final Map<String, DimensionDefinition> dims = new ConcurrentHashMap<>();
    private final Map<String, MeasureDefinition> measures = new ConcurrentHashMap<>();

    // per-template maps: templateId -> set of ids (order preserved)
    private final Map<String, Set<String>> templateToDims = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> templateToMeasures = new ConcurrentHashMap<>();

    @Override
    public Optional<DimensionDefinition> getDimension(String id) {
        return Optional.ofNullable(dims.get(id));
    }

    @Override
    public Optional<MeasureDefinition> getMeasure(String id) {
        return Optional.ofNullable(measures.get(id));
    }

    @Override
    public List<DimensionDefinition> listDimensions(String templateId) {
        if (templateId == null) return new ArrayList<>(dims.values());
        return templateToDims.getOrDefault(templateId, Set.of()).stream()
            .map(dims::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<MeasureDefinition> listMeasures(String templateId) {
        if (templateId == null) return new ArrayList<>(measures.values());
        return templateToMeasures.getOrDefault(templateId, Set.of()).stream()
            .map(measures::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void registerDimension(DimensionDefinition def, String templateId) {
        dims.put(def.getId(), def);
        if (templateId != null)
            templateToDims.computeIfAbsent(templateId, t ->
                    Collections.synchronizedSet(new LinkedHashSet<>()))
                .add(def.getId());
    }

    @Override
    public void registerMeasure(MeasureDefinition def, String templateId) {
        measures.put(def.getId(), def);
        if (templateId != null)
            templateToMeasures.computeIfAbsent(templateId, t -> Collections.synchronizedSet(new LinkedHashSet<>()))
                .add(def.getId());
    }
}
