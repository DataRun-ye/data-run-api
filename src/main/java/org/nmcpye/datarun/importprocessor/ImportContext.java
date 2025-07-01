package org.nmcpye.datarun.importprocessor;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada 14/05/2025 (7amza.it@gmail.com)
 */
@Accessors(fluent = true)
@Getter
@ToString
public class ImportContext<T> {
    private final boolean dryRun;
    private final List<T> rawData;
    private final List<T> processed = new ArrayList<>();
    private final List<ImportMessage> messages = new ArrayList<>();

    public ImportContext(List<T> rawData, boolean dryRun) {
        this.rawData = rawData;
        this.dryRun = dryRun;
    }

    public void addProcessed(T item) {
        processed.add(item);
    }

    public void addMessage(ImportMessage message) {
        messages.add(message);
    }
}
