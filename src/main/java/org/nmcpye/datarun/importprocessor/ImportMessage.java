package org.nmcpye.datarun.importprocessor;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <14-05-2025>
 */
public class ImportMessage {
    public enum Severity { INFO, WARNING, ERROR }
    private final String message;
    private final Severity severity;

    public ImportMessage(String message, Severity severity) {
        this.message = message;
        this.severity = severity;
    }

    public String getMessage() { return message; }
    public Severity getSeverity() { return severity; }
}
