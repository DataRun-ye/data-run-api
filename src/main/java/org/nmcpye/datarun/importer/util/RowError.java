package org.nmcpye.datarun.importer.util;

import java.util.List;

/**
 * @author Hamza Assada 02/06/2025 (7amza.it@gmail.com)
 */
public class RowError {
    private int rowIndex;
    private List<String> messages;

    public RowError(int rowIndex, List<String> messages) {
        this.rowIndex = rowIndex;
        this.messages = messages;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}
