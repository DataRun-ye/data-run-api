package org.nmcpye.datarun.importprocessor;

import java.util.List;

/**
 * @author Hamza Assada (14-05-2025), <7amza.it@gmail.com>
 */
public class ImportResult {
    private final List<ImportMessage> messages;
    private final boolean dryRun;

    public ImportResult(List<ImportMessage> messages, boolean dryRun) {
        this.messages = messages;
        this.dryRun = dryRun;
    }

    public List<ImportMessage> getMessages() { return messages; }
    public boolean isDryRun() { return dryRun; }
}

