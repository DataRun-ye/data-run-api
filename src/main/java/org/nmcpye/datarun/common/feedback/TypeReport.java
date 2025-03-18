package org.nmcpye.datarun.common.feedback;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TypeReport implements ErrorReportContainer {
    private static final Map<Class<?>, TypeReport> EMPTY_BY_TYPE = new ConcurrentHashMap<>();

    private final Class<?> klass;

    private final boolean empty;

    private final Stats stats = new Stats();

    private final Map<Integer, ObjectReport> objectReportMap = new HashMap<>();

    @JsonCreator
    public TypeReport(@JsonProperty("klass") Class<?> klass) {
        this(klass, false);
    }

    private TypeReport(Class<?> klass, boolean empty) {
        this.klass = klass;
        this.empty = empty;
    }

    public static TypeReport empty(Class<?> klass) {
        return EMPTY_BY_TYPE.computeIfAbsent(klass, type -> new TypeReport(type, true));
    }

    public final boolean isEmptySingleton() {
        return empty;
    }

    // -----------------------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------------------

    public TypeReport mergeAllowEmpty(TypeReport other) {
        if (isEmptySingleton()) {
            return other;
        }
        if (other.isEmptySingleton()) {
            return this;
        }
        merge(other);
        return this;
    }

    public void merge(TypeReport other) {
        if (empty) {
            throw new IllegalStateException("Empty report cannot be changed.");
        }
        if (other.empty) {
            return; // done: nothing to merge with
        }
        stats.merge(other.getStats());

        other.objectReportMap.forEach(
            (index, objectReport) -> objectReportMap.compute(index, (key, value) -> {
                if (value == null) {
                    return objectReport;
                }
                objectReport.forEachErrorReport(value::addErrorReport);
                return value;
            }));
    }

    /**
     * Removes entries where the {@link ObjectReport} has no
     * {@link ErrorReport}s.
     */
    public void clean() {
        objectReportMap.entrySet().removeIf(entry -> !entry.getValue().hasErrorReports());
    }

    public void addObjectReport(ObjectReport report) {
        objectReportMap.compute(report.getIndex(),
            (key, value) -> value == null ? report : value.merge(report));
    }

    // -----------------------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------------------

    @JsonProperty
    public Class<?> getKlass() {
        return klass;
    }

    @JsonProperty
    public Stats getStats() {
        return stats;
    }

    @JsonProperty
    public List<ObjectReport> getObjectReports() {
        return new ArrayList<>(objectReportMap.values());
    }

    @JsonProperty
    public void setObjectReports(List<ObjectReport> objectReports) {
        objectReportMap.clear();
        if (objectReports != null) {
            objectReports.forEach(or -> objectReportMap.put(or.getIndex(), or));
        }
    }

    @JsonIgnore
    public int getObjectReportsCount() {
        return objectReportMap.size();
    }

    public boolean hasObjectReports() {
        return !objectReportMap.isEmpty();
    }

    public ObjectReport getFirstObjectReport() {
        return objectReportMap.isEmpty() ? null : objectReportMap.values().iterator().next();
    }

    public void forEachObjectReport(Consumer<ObjectReport> reportConsumer) {
        objectReportMap.values().forEach(reportConsumer);
    }

    @JsonIgnore
    @Override
    public int getErrorReportsCount() {
        return objectReportMap.values().stream().mapToInt(ObjectReport::getErrorReportsCount).sum();
    }

    @Override
    public int getErrorReportsCount(ErrorCode errorCode) {
        return objectReportMap.values().stream().mapToInt(report -> report.getErrorReportsCount(errorCode)).sum();
    }

    @Override
    public boolean hasErrorReports() {
        return objectReportMap.values().stream().anyMatch(ObjectReport::hasErrorReports);
    }

    @Override
    public boolean hasErrorReport(Predicate<ErrorReport> test) {
        return objectReportMap.values().stream().anyMatch(report -> report.hasErrorReport(test));
    }

    @Override
    public void forEachErrorReport(Consumer<ErrorReport> reportConsumer) {
        objectReportMap.values().forEach(objectReport -> objectReport.forEachErrorReport(reportConsumer));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("klass", klass)
            .add("stats", stats)
            .add("objectReports", getObjectReports())
            .toString();
    }

}
