package org.nmcpye.datarun.common.feedback;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface ErrorReportContainer {
    int getErrorReportsCount();

    int getErrorReportsCount(ErrorCode errorCode);

    boolean hasErrorReports();

    boolean hasErrorReport(Predicate<ErrorReport> test);

    void forEachErrorReport(Consumer<ErrorReport> reportConsumer);
}
