package org.nmcpye.datarun.common.feedback;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.nmcpye.datarun.drun.postgres.common.Identifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ObjectReport implements ErrorReportContainer {
    private final Class<?> klass;

    private final Integer index;
    private final Map<ErrorCode, List<ErrorReport>> errorReportsByCode = new EnumMap<>(ErrorCode.class);
    /**
     * UID of object (if object is id object).
     */
    private String uid;
    /**
     * Name to be used if ImportReportMode is DEBUG
     */
    private String displayName;

    public ObjectReport(@Nonnull Identifiable object, @Nonnull ObjectIndexProvider objectIndexProvider) {
        this(object, objectIndexProvider, null);
    }

    public ObjectReport(@Nonnull Identifiable object, @Nonnull ObjectIndexProvider objectIndexProvider,
                        @Nullable String uid) {
        this(object.getClass(), objectIndexProvider.mergeObjectIndex(object), uid == null ? object.getUid() : uid);
    }

    @JsonCreator
    public ObjectReport(@JsonProperty("klass") Class<?> klass, @JsonProperty("index") Integer index) {
        this.klass = klass;
        this.index = index;
    }

    public ObjectReport(Class<?> klass, Integer index, String uid) {
        this.klass = klass;
        this.index = index;
        this.uid = uid;
    }

    public ObjectReport(Class<?> klass, Integer index, String uid, String displayName) {
        this.klass = klass;
        this.index = index;
        this.uid = uid;
        this.displayName = displayName;
    }

    public ObjectReport(ObjectReport objectReport) {
        this.klass = objectReport.getKlass();
        this.index = objectReport.getIndex();
        this.uid = objectReport.getUid();
        this.displayName = objectReport.getDisplayName();
    }

    public ObjectReport(Class<?> klass, int index, List<ErrorReport> errorReports) {
        this(klass, index);
        errorReports.forEach(this::addErrorReport);
    }

    // -----------------------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------------------

    public ObjectReport merge(ObjectReport objectReport) {
        if (objectReport == this) {
            return this;
        }

        objectReport.forEachErrorReport(this::addErrorReport);

        return this;
    }

    public void addErrorReports(List<? extends ErrorReport> errorReports) {
        errorReports.forEach(this::addErrorReport);
    }

    public void addErrorReport(ErrorReport errorReport) {
        errorReportsByCode.computeIfAbsent(errorReport.getErrorCode(), key -> new ArrayList<>()).add(errorReport);
    }

    // -----------------------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------------------

    @JsonProperty
    public Class<?> getKlass() {
        return klass;
    }

    @JsonProperty
    public Integer getIndex() {
        return index;
    }

    @JsonProperty
    public String getUid() {
        return uid;
    }

    @JsonProperty
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @JsonProperty
    public List<ErrorReport> getErrorReports() {
        List<ErrorReport> errorReports = new ArrayList<>();
        forEachErrorReport(errorReports::add);
        return errorReports;
    }

    @JsonProperty
    public void setErrorReports(List<ErrorReport> errorReports) {
        errorReportsByCode.clear();
        if (errorReports != null) {
            errorReports.forEach(
                er -> errorReportsByCode.computeIfAbsent(er.getErrorCode(), key -> new ArrayList<>()).add(er));
        }
    }

    @JsonIgnore
    @Override
    public int getErrorReportsCount() {
        return errorReportsByCode.values().stream().mapToInt(List::size).sum();
    }

    @Override
    public int getErrorReportsCount(ErrorCode errorCode) {
        List<ErrorReport> reports = errorReportsByCode.get(errorCode);
        return reports == null ? 0 : reports.size();
    }

    @Override
    public boolean hasErrorReports() {
        return !errorReportsByCode.isEmpty();
    }

    @Override
    public boolean hasErrorReport(Predicate<ErrorReport> test) {
        return errorReportsByCode.values().stream().anyMatch(reports -> reports.stream().anyMatch(test));
    }

    @Override
    public void forEachErrorReport(Consumer<ErrorReport> reportConsumer) {
        errorReportsByCode.values().forEach(reports -> reports.forEach(reportConsumer));
    }

    public boolean isEmpty() {
        return errorReportsByCode.isEmpty();
    }

    public int size() {
        return errorReportsByCode.size();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("klass", klass)
            .add("index", index)
            .add("uid", uid)
            .add("errorReports", getErrorReports())
            .toString();
    }
}
