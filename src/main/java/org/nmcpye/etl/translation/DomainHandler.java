package org.nmcpye.etl.translation;

public interface DomainHandler {
    String getDomainName(); // e.g., "INVENTORY"
    void process(SubmissionContext context, FlowConfig config);
}
