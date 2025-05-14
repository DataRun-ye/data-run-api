package org.nmcpye.datarun.importprocessor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <14-05-2025>
 */
public class ImportContext<T> {
    private final List<T> rawData;
    private final List<T> processed = new ArrayList<>();
    private final List<ImportMessage> messages = new ArrayList<>();
    private final boolean dryRun;

    public ImportContext(List<T> rawData, boolean dryRun) {
        this.rawData = rawData;
        this.dryRun = dryRun;
    }

    public List<T> getRawData() {
        return rawData;
    }

    public List<T> getProcessed() {
        return processed;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void addProcessed(T item) {
        processed.add(item);
    }

    public void addMessage(ImportMessage message) {
        messages.add(message);
    }

    public List<ImportMessage> getMessages() {
        return messages;
    }
}
