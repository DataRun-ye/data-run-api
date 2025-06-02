package org.nmcpye.datarun.importprocessor;

import lombok.Getter;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada (14-05-2025), <7amza.it@gmail.com>
 */
@Getter
public class ImportContext<T> {
    private final List<T> rawData;
    private final List<T> processed = new ArrayList<>();
    @Singular
    private final List<ImportMessage> messages = new ArrayList<>();
    private final boolean dryRun;

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
